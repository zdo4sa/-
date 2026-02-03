// パッケージ宣言：このクラスの属するパッケージを指定
package com.example.twentysix.controller;

// IO 例外：CSV 出力時のストリーム操作で発生しうる
import java.io.IOException;
// レスポンスへ直接文字列を書き出すための Writer
import java.io.PrintWriter;
// 日付型（年-月-日）
import java.time.LocalDate;
// 時刻型（時:分:秒）
import java.time.LocalTime;
// 一覧表示などで使うコレクション
import java.util.List;

// サーブレットの HTTP レスポンスを扱う（CSV ダウンロードで使用）
import jakarta.servlet.http.HttpServletResponse;

// 日付・時刻のフォーマットをリクエストパラメータに適用するアノテーション
import org.springframework.format.annotation.DateTimeFormat;
// メソッドレベルの権限制御に利用（クラスに付与された @PreAuthorize を有効にする前提）
import org.springframework.security.access.prepost.PreAuthorize;
// MVC のコントローラクラスであることを示す
import org.springframework.stereotype.Controller;
// テンプレートへ値を受け渡すためのコンテナ
import org.springframework.ui.Model;
// ルーティング系アノテーション（HTTP メソッドやパスをマッピング）
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// 予約エンティティ：予約一覧や統計の取得で使用
import com.example.twentysix.entity.Reservation;
// シフトエンティティ：全体のシフト管理に使用
import com.example.twentysix.entity.Shift;
import com.example.twentysix.repository.ReservationRepository;
import com.example.twentysix.repository.SurveyResponseRepository;
// ユーザ検索のためのリポジトリ（スタッフ一覧など）
import com.example.twentysix.repository.UserRepository;
// 予約に関するビジネスロジックを提供するサービス
import com.example.twentysix.service.ReservationService;
// シフトに関するビジネスロジックを提供するサービス
import com.example.twentysix.service.ShiftService;

//このクラスが MVC のコントローラであることを宣言
@Controller
//すべてのハンドラメソッドの先頭に /admin を付与する
@RequestMapping("/admin")
//このクラスの全メソッドに対し、ADMIN ロールを要求（クラスレベルのガード）
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
	//予約関連の業務処理にアクセスするためのサービス
	private final ReservationService reservationService;
	//シフト関連の業務処理にアクセスするためのサービス
	private final ShiftService shiftService;
	//ユーザ情報（特に STAFF ロールユーザの一覧）取得に使用するリポジトリ
	private final UserRepository userRepository;
	private final SurveyResponseRepository surveyResponseRepository;
	private final ReservationRepository reservationRepository;

	//コンストラクタインジェクション：必要な依存を受け取ってフィールドに設定
	public AdminController(ReservationService reservationService,
			ShiftService shiftService,
			UserRepository userRepository,
			SurveyResponseRepository surveyResponseRepository,
			ReservationRepository reservationRepository) { // ←ここに追加！

		this.reservationService = reservationService;
		this.shiftService = shiftService;
		this.userRepository = userRepository;

		// これで、引数で受け取ったリポジトリをフィールドに正しく代入できます
		this.surveyResponseRepository = surveyResponseRepository;
		this.reservationRepository = reservationRepository;
	}

	//全予約一覧画面を表示するハンドラ（期間フィルタの有無に応じて出し分け）
	@GetMapping("/reservations")
	public String listAllReservations(
			//開始日の任意指定（ISO 形式の yyyy-MM-dd を想定）
			@RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			//終了日の任意指定（ISO 形式の yyyy-MM-dd を想定）
			@RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			//画面へ値を渡すためのモデル
			Model model) {
		//取得した予約一覧を受けるためのリスト
		List<Reservation> reservations;
		//期間が指定されている場合は範囲検索
		if (startDate != null && endDate != null) {
			reservations = reservationService.getReservationsByDateRange(startDate, endDate);
		} else {
			//未指定の場合は全件取得
			reservations = reservationService.getAllReservations();
		}
		//テンプレートに予約一覧を渡す（th:each でループ表示）
		model.addAttribute("allReservations", reservations);
		//レンダリングするテンプレート名（resources/templates/admin_reservations.html）
		return "admin_reservations";

	}

	//全スタッフのシフト一覧・登録画面を表示するハンドラ
	@GetMapping("/shifts")
	public String listAllShifts(
			//フィルタ用の開始日（任意）
			@RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			//フィルタ用の終了日（任意）
			@RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			//画面表示に使用するモデル

			Model model) {
		//取得したシフト一覧を受ける
		List<Shift> shifts;
		//期間が指定されていれば期間内のみ
		if (startDate != null && endDate != null) {
			shifts = shiftService.getShiftsByDateRange(startDate, endDate);
		} else {
			//指定がなければ全件
			shifts = shiftService.getAllShifts();
		}
		//シフト一覧をモデルに登録
		model.addAttribute("allShifts", shifts);
		//スタッフ選択用のプルダウン表示に利用：STAFF ロールのユーザ一覧
		model.addAttribute("staffs", userRepository.findByRole("STAFF"));
		//レンダリングするテンプレート名（admin_shifts.html）

		return "admin_shifts";

	}

	//管理者によるシフトの作成・更新（POST）
	@PostMapping("/shifts/create-update")
	public String createOrUpdateShiftByAdmin(
			//対象スタッフ ID（必須）
			@RequestParam("staffId") Long staffId,
			//シフト日（ISO 形式、必須）
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			//開始時刻（ISO 形式、必須）
			@RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
			//終了時刻（ISO 形式、必須）
			@RequestParam("endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {
		// サービスに委譲して作成または更新を実行（同日既存なら上書き）
		shiftService.createOrUpdateShift(staffId, date, startTime, endTime);
		// 正常終了後、成功クエリパラメータを付けて一覧画面へリダイレクト
		return "redirect:/admin/shifts?success=shiftUpdated";
	}

	// 管理者によるシフト削除（POST）
	@PostMapping("/shifts/{id}/delete")
	public String deleteShiftByAdmin(@PathVariable("id") Long shiftId) {
		// 指定 ID のシフトを削除
		shiftService.deleteShift(shiftId);
		// 削除完了メッセージを付けて一覧へ戻る
		return "redirect:/admin/shifts?success=shiftDeleted";
	}

	// 予約統計の表示（期間未指定時は直近 1 か月をデフォルトに）
	@GetMapping("/statistics")
	public String showStatistics(
			// 開始日（任意、未指定時は 1 か月前）
			@RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			// 終了日（任意、未指定時は当日）
			@RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			// テンプレートに値を渡すためのモデル
			Model model) {
		// デフォルト開始日：今日から 1 か月前
		if (startDate == null)
			startDate = LocalDate.now().minusMonths(1);
		// デフォルト終了日：今日
		if (endDate == null)
			endDate = LocalDate.now();
		// 期間の再表示用にモデルへ格納
		model.addAttribute("startDate", startDate);
		model.addAttribute("endDate", endDate);
		// メニュー別の予約件数マップを取得しモデルへ
		model.addAttribute("reservationCountByMenu", reservationService.getReservationCountByMenu(startDate, endDate));
		// スタッフ別の予約件数マップを取得しモデルへ
		model.addAttribute("reservationCountByStaff",
				reservationService.getReservationCountByStaff(startDate, endDate));
		// 統計画面テンプレート（admin_statistics.html）を表示
		return "admin_statistics";
	}

	// 予約統計の CSV エクスポート（ダウンロードレスポンス）
	@GetMapping("/statistics/csv")
	public void exportStatisticsCsv(
			// CSV 出力対象の開始日（未指定時は 1 か月前）
			@RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			// CSV 出力対象の終了日（未指定時は 当日）
			@RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			// HTTP レスポンス（ヘッダ設定と書き出しに使う）
			HttpServletResponse response) throws IOException {
		// デフォルト期間の設定（画面表示と同様のロジック）
		if (startDate == null)
			startDate = LocalDate.now().minusMonths(1);
		if (endDate == null)
			endDate = LocalDate.now();
		// CSV としてダウンロードさせるためのコンテントタイプとヘッダを設定（UTF-8 明示）
		response.setContentType("text/csv; charset=UTF-8");
		response.setHeader("Content-Disposition", "attachment; filename=\"reservation_statistics.csv\"");
		// try-with-resources で Writer を自動クローズ
		try (PrintWriter writer = response.getWriter()) {
			// 見出し行：統計期間
			writer.append("統計期間: " + startDate + " から " + endDate + "\n\n");
			// セクション：メニュー別件数
			writer.append("メニュー別予約数\n");
			// 予約サービスからメニュー別件数を取得して 1 行ずつ出力（menu,count）
			reservationService.getReservationCountByMenu(startDate, endDate).forEach((menu, count) -> {
				writer.append(menu + "," + count + "\n");
			});
			// 区切りの空行
			writer.append("\n スタッフ別予約数\n");
			// スタッフ別件数を同様に出力（staff,count）
			reservationService.getReservationCountByStaff(startDate, endDate).forEach((staff, count) -> {
				writer.append(staff + "," + count + "\n");
			});
		}
	}

	@GetMapping("/surveys")
	public String viewSurveys(Model model) {
		model.addAttribute("responses", surveyResponseRepository.findAllByOrderByIdDesc());
		return "admin_surveys";
	}

	// 修正：サービス層のメソッドを呼ぶように変更
	@PostMapping("/reservations/{id}/delete")
	public String deleteReservationByAdmin(@PathVariable("id") Long id) {
		// リポジトリ直ではなく、作成したサービスメソッドを呼び出す
		reservationService.deleteReservation(id);
		return "redirect:/admin/reservations?success=deleted";
	}

	@GetMapping("/dashboard")
	public String adminDashboard() {
		return "admin_dashboard";
	}

}
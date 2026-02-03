//パッケージ宣言：予約に関する Web ルーティング一式
package com.example.twentysix.controller;

//予約日時の型（LocalDate/LocalTime）やコレクション
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

//リクエストパラメータの日付/時間文字列を Java 時間型に変換するためのアノテーション
import org.springframework.format.annotation.DateTimeFormat;
//認証済みユーザの principal をメソッド引数に受け取る
import org.springframework.security.core.annotation.AuthenticationPrincipal;
//Spring Security の標準ユーザ表現
import org.springframework.security.core.userdetails.UserDetails;
//MVC コントローラ宣言
import org.springframework.stereotype.Controller;
//テンプレートに値を受け渡すためのモデル
import org.springframework.ui.Model;
//ルーティング系アノテーション（GET/POST/パス変数など）
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

//予約エンティティ：フォームバインドや再表示で利用
import com.example.twentysix.entity.Reservation;
import com.example.twentysix.entity.SurveyResponse;
//ユーザエンティティ：顧客・スタッフの紐付けに使用
import com.example.twentysix.entity.User;
//ユーザ検索のためのリポジトリ（メール→User、ID→User）
import com.example.twentysix.repository.UserRepository;
//予約に関する業務ロジック（重複予約チェック、作成・更新・キャンセル等）
import com.example.twentysix.service.ReservationService;
import com.example.twentysix.service.SurveyService;

//コントローラであることを表明
@Controller
//予約関連の URL の先頭プレフィックスを /reservation に統一
@RequestMapping("/reservation")
public class ReservationController {
	private final ReservationService reservationService;
	private final UserRepository userRepository;
	private final SurveyService surveyService;

	// 1. コンストラクタの引数に SurveyService surveyService を追加する
	public ReservationController(ReservationService reservationService,
			UserRepository userRepository,
			SurveyService surveyService) {

		this.reservationService = reservationService;
		this.userRepository = userRepository;

		// 2. 引数で受け取った surveyService をフィールドに代入する
		this.surveyService = surveyService;
	}

	// 予約登録フォームの表示（空フォーム + スタッフ一覧）
	@GetMapping("/new")
	public String showReservationForm(Model model) {
		// スタッフ一覧をプルダウン用に投入
		model.addAttribute("staffs", reservationService.getAllStaffs());
		// 新規作成用の空の Reservation をバインド（th:object 相当）
		model.addAttribute("reservation", new Reservation()); // For form binding
		// 予約フォームテンプレートへ
		return "reservation_form";

	}

	// 予約作成の受付（POST）：顧客認証前提
	@PostMapping("/new")
	public String createReservation(
			// ログイン中ユーザ（UserDetails）を注入（メールが username）
			@AuthenticationPrincipal UserDetails userDetails,
			// スタッフ選択（ID 指定）
			@RequestParam("staffId") Long staffId,
			// 日付（yyyy-MM-dd 形式を LocalDate へ変換）
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			// 時刻（HH:mm 形式を LocalTime へ変換）
			@RequestParam("timeSlot") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime timeSlot,
			// メニュー名（自由入力）
			@RequestParam("menu") String menu,
			// 画面再表示時のエラーメッセージや再入力値セットに使用
			Model model) {
		// ログイン中の顧客をメールから取得。見つからない場合は例外
		User customer = userRepository.findByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("Customer not found"));
		try {
			// ビジネスルールに従って予約作成（重複・シフト内判定を内部で実施）
			reservationService.createReservation(customer, staffId, date, timeSlot, menu);
			// 正常完了：履歴画面へ success クエリを付けてリダイレクト
			return "redirect:/reservation/history?success=created";
		} catch (IllegalStateException e) {
			// 競合やバリデーションエラーなどビジネス例外を画面に返す
			model.addAttribute("errorMessage", e.getMessage());
			// スタッフ一覧を再投入（フォーム再表示で必要）
			model.addAttribute("staffs", reservationService.getAllStaffs());
			// 入力値を保持するための一時 Reservation を作成し、フォームに再表示
			Reservation tempReservation = new Reservation();
			// スタッフ ID からエンティティへ（存在しない場合は null 設定）
			tempReservation.setStaff(userRepository.findById(staffId).orElse(null));
			// 入力日付を保持
			tempReservation.setRecordDate(date);
			// 入力時刻を保持
			tempReservation.setTimeSlot(timeSlot);
			// 入力メニューを保持
			tempReservation.setMenu(menu);
			// モデルへ再投入（テンプレートは reservation_form を再利用）
			model.addAttribute("reservation", tempReservation);
			// エラー時も同じフォームを表示して再入力を促す
			return "reservation_form";
		}
	}

	// 自分の予約履歴を一覧表示（ログインユーザに紐付く）
	@GetMapping("/history")
	public String showReservationHistory(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		// ログイン中顧客を取得
		User customer = userRepository.findByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("Customer not found"));
		// 顧客の予約一覧（新しい順）をモデルへ
		model.addAttribute("userReservations", reservationService.getUserReservations(customer));
		// 履歴画面テンプレートへ
		return "reservation_history";
	}

	// 予約編集フォームの表示（予約 ID 指定）
	@GetMapping("/{id}/edit")
	public String showEditReservationForm(@PathVariable("id") Long reservationId, Model model) {
		// 予約を ID で検索。存在しなければ不正 ID 例外
		Reservation reservation = reservationService.getReservationById(reservationId)
				.orElseThrow(() -> new IllegalArgumentException("Invalid reservation Id:" + reservationId));
		// 編集対象の予約をモデルへ
		model.addAttribute("reservation", reservation);
		// スタッフ再割当てを想定し、スタッフ一覧も渡す
		model.addAttribute("staffs", reservationService.getAllStaffs()); // For staff to re-assign if needed
		// 新規と同じフォームテンプレートを再利用
		return "reservation_form"; // Re-use form for editing
	}

	// 予約の更新（POST）
	@PostMapping("/{id}/edit")
	public String updateReservation(
			// 編集対象の予約 ID
			@PathVariable("id") Long reservationId,
			// 新しい日付
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			// 新しい時間
			@RequestParam("timeSlot") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime timeSlot,
			// 新しいメニュー名
			@RequestParam("menu") String menu,
			// エラー時の再表示等に使用
			Model model) {
		try {
			// ビジネスルールに従い予約を更新（重複チェック・シフト内チェック含む）
			reservationService.updateReservation(reservationId, date, timeSlot, menu);
			// 正常完了：履歴画面へ success=updated を付けて戻る
			return "redirect:/admin/reservations?success=updated";

		} catch (IllegalStateException e) {
			// 業務例外（時間競合など）を画面に表示
			model.addAttribute("errorMessage", e.getMessage());
			// 編集対象の最新状態を取得（見つからなければ空の Reservation）
			model.addAttribute("reservation",
					reservationService.getReservationById(reservationId).orElse(new Reservation()));
			// スタッフ一覧も再投入
			model.addAttribute("staffs", reservationService.getAllStaffs());
			// 同じフォームを再表示
			return "reservation_form";
		}
	}

	// 予約のキャンセル（ステータス変更）
	@PostMapping("/{id}/cancel")
	public String cancelReservation(@PathVariable("id") Long reservationId) {
		// ステータスを「キャンセル済」に変更して保存
		reservationService.cancelReservation(reservationId);
		// 履歴画面へ success=cancelled を付けて戻る
		return "redirect:/reservation/history?success=cancelled";
	}

	// 指定スタッフ・日付における利用可能な時間枠を JSON で返す（AJAX 用）
	@GetMapping("/available-slots")
	@ResponseBody // 戻り値をビュー名ではなく、HTTP ボディ（JSON）として返却
	public List<LocalTime> getAvailableSlots(
			// スタッフ ID（必須）
			@RequestParam("staffId") Long staffId,
			// 日付（必須、ISO 形式を LocalDate に変換）
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) { // Add @DateTimeFormat
		// 予約サービスで空き枠を計算して返す（30 分刻み、シフト内、既予約除外）
		return reservationService.getAvailableTimeSlots(staffId, date);
	}

	@GetMapping("/{id}/survey")
	public String showSurveyForm(@PathVariable("id") Long reservationId, Model model) {
		// 予約の存在確認と回答済みチェックはサービス層に任せる
		Reservation reservation = reservationService.getReservationById(reservationId)
				.orElseThrow(() -> new IllegalArgumentException("予約が見つかりません。"));

		model.addAttribute("reservation", reservation);
		// 新しい回答オブジェクトをバインド
		model.addAttribute("surveyResponse", new SurveyResponse());
		return "survey_form"; // survey_form.html を作成する
	}

	// アンケート回答受付
	@PostMapping("/{id}/survey")
	public String submitSurvey(@PathVariable("id") Long reservationId,
			@RequestParam("staffRating") Integer staffRating,
			@RequestParam("serviceRating") Integer serviceRating,
			@RequestParam("comment") String comment,
			Model model) {
		try {
			surveyService.saveSurveyResponse(reservationId, staffRating, serviceRating, comment);
			return "redirect:/reservation/history?success=surveySubmitted";
		} catch (IllegalStateException | IllegalArgumentException e) {
			model.addAttribute("errorMessage", e.getMessage());
			// エラー時もフォームを再表示できるようにモデル属性を再投入
			model.addAttribute("reservation", reservationService.getReservationById(reservationId).get());
			model.addAttribute("surveyResponse", new SurveyResponse());
			return "survey_form";
		}
	}

}
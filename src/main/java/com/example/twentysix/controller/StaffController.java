package com.example.twentysix.controller;

//日付・時刻型とコレクション
import java.time.LocalDate;
import java.time.LocalTime;

//リクエストの日時パラメータを Java 時間型にバインドするためのアノテーション
import org.springframework.format.annotation.DateTimeFormat;
//メソッド／クラスレベルの権限制御アノテーションを評価（SecurityConfig 側で有効化が前提）
import org.springframework.security.access.prepost.PreAuthorize;
//認証済みユーザの principal をメソッド引数へインジェクション
import org.springframework.security.core.annotation.AuthenticationPrincipal;
//Spring Security のユーザ情報インタフェース
import org.springframework.security.core.userdetails.UserDetails;
//MVC コントローラ宣言
import org.springframework.stereotype.Controller;
//テンプレートに値を受け渡すためのモデル
import org.springframework.ui.Model;
//ルーティング系アノテーション（GET/POST、パス変数、クエリ等）
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

//パッケージ宣言：このコントローラの属する論理パッケージ
//予約エンティティ：一覧・編集・キャンセル等で使用
import com.example.twentysix.entity.Reservation;
//ユーザエンティティ：ログイン中スタッフや担当者の取得に使用
import com.example.twentysix.entity.User;
//予約検索のための JPA リポジトリ（スタッフ自身の予約一覧に使用）
import com.example.twentysix.repository.ReservationRepository;
//ユーザ検索のための JPA リポジトリ（メール→User 解決）
import com.example.twentysix.repository.UserRepository;
//予約のビジネスロジック（更新・キャンセル等）
import com.example.twentysix.service.ReservationService;
//シフトのビジネスロジック（登録・削除・一覧）
import com.example.twentysix.service.ShiftService;

//コントローラクラスであることを宣言
@Controller
//このコントローラ配下の URL 先頭を /staff に固定（役割別の画面）
@RequestMapping("/staff")
//STAFF or ADMIN ロールのいずれかを要求（クラス全体に適用）
@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
public class StaffController {
	// 予約業務ロジック：更新・キャンセル・取得などを提供
	private final ReservationService reservationService;
	// シフト業務ロジック：登録・更新・削除・取得などを提供
	private final ShiftService shiftService;
	// ユーザ検索のためのリポジトリ（ログイン中スタッフ解決に使用）
	private final UserRepository userRepository;
	// スタッフ自身の予約一覧を取得するために使用
	private final ReservationRepository reservationRepository;

	// 依存関係をコンストラクタ DI（テスト容易性・不変性のため final）
	public StaffController(ReservationService reservationService, ShiftService shiftService,
			UserRepository userRepository, ReservationRepository reservationRepository) {
		// フィールドへ予約サービスを設定
		this.reservationService = reservationService;
		// フィールドへシフトサービスを設定
		this.shiftService = shiftService;
		// フィールドへユーザリポジトリを設定
		this.userRepository = userRepository;
		// フィールドへ予約リポジトリを設定
		this.reservationRepository = reservationRepository;
	}

	// スタッフ担当の予約一覧画面（自身に紐づく予約を新しい順で表示）
	@GetMapping("/reservations")
	public String listStaffReservations(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		// ログイン中のユーザ（メール）からスタッフエンティティを取得。見つからない場合は例外
		User staff = userRepository.findByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("Staff not found"));
		// スタッフに紐づく予約一覧（date desc, timeSlot desc）をモデルへ
		model.addAttribute("staffReservations",
				reservationRepository.findByStaffOrderByRecordDateDescTimeSlotDesc(staff));
		// テンプレート staff_reservations.html をレンダリング
		return "staff_reservations";

	}

	// スタッフが予約編集フォームを表示（予約 ID 指定）
	@GetMapping("/reservations/{id}/edit")
	public String showEditReservationFormByStaff(@PathVariable("id") Long reservationId, Model model) {
		// 対象予約を取得（存在しなければ不正 ID 例外）
		Reservation reservation = reservationService.getReservationById(reservationId)
				.orElseThrow(() -> new IllegalArgumentException("Invalid reservation Id:" + reservationId));
		// 編集対象予約をモデルへ
		model.addAttribute("reservation", reservation);
		// 再割当て等に備えスタッフ一覧をモデルへ（共通フォーム再利用のため）
		model.addAttribute("staffs", reservationService.getAllStaffs()); // For staff to re-assign if needed
		// 新規と同じ reservation_form を再利用
		return "reservation_form"; // Re-use customer form for editing
	}

	// スタッフによる予約更新（POST）
	@PostMapping("/reservations/{id}/edit")
	public String updateReservationByStaff(
			// 更新対象の予約 ID（パスから取得）
			@PathVariable("id") Long reservationId,
			// 新しい日付（ISO 形式→LocalDate）
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			// 新しい時間（ISO 形式→LocalTime）
			@RequestParam("timeSlot") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime timeSlot,
			// 新しいメニュー名
			@RequestParam("menu") String menu,
			// エラー時の再表示用
			@AuthenticationPrincipal UserDetails userDetails, // ★追加：ログイン情報を取得
			Model model) {
		try {
			// 予約更新（重複／シフト内チェックはサービス層で実施）
			reservationService.updateReservation(reservationId, date, timeSlot, menu);
			// ★権限判定でリダイレクト先を出し分ける
			if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
				// 管理者の場合は「全予約一覧」へ
				return "redirect:/admin/reservations?success=updated";
			}
			// 正常終了：自身の予約一覧へ success=updated を付けて戻る
			return "redirect:/staff/reservations?success=updated";
		} catch (IllegalStateException e) {
			// ビジネス例外（時間競合等）を画面に表示
			model.addAttribute("errorMessage", e.getMessage());
			// 最新の予約状態を再取得（見つからない時は空オブジェクト）
			model.addAttribute("reservation",
					reservationService.getReservationById(reservationId).orElse(new Reservation()));
			// スタッフ一覧も再投入
			model.addAttribute("staffs", reservationService.getAllStaffs());
			// 同じフォームで再入力を促す
			return "reservation_form";
		}
	}

	// スタッフによる予約キャンセル（POST）
	@PostMapping("/reservations/{id}/cancel")
	public String cancelReservationByStaff(@PathVariable("id") Long reservationId) {
		// ステータスを「キャンセル済」に更新し保存
		reservationService.cancelReservation(reservationId);
		// 自身の予約一覧へ success=cancelled を付けて戻る
		return "redirect:/staff/reservations?success=cancelled";
	}

	// スタッフ向けのシフト一覧・登録画面（自分のシフトのみ）
	// スタッフ自身のシフト管理画面を表示
	@GetMapping("/shifts")
	public String showMyShiftManagement(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		// 1. ログイン中のスタッフ情報を取得
		User staff = userRepository.findByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("Staff not found"));

		// 2. 画面に渡すデータをセット（HTML側の変数名 staffShifts に合わせる）
		model.addAttribute("staffId", staff.getId());
		model.addAttribute("staffShifts", shiftService.getShiftsByStaff(staff));

		return "staff_shift_management";
	}

	// スタッフ自身のシフト登録・更新（POST）
	@PostMapping("/shifts/create-update")
	public String createOrUpdateStaffShift(
			@RequestParam("staffId") Long staffId,
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
			@RequestParam("endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
			Model model) {
		try {
			// サービスを呼び出し（時間の逆転バリデーションはサービス層で実施）
			shiftService.createOrUpdateShift(staffId, date, startTime, endTime);
			return "redirect:/staff/shifts?success=shiftUpdated";

		} catch (IllegalArgumentException e) {
			// 500エラーを回避：メッセージを画面に渡す
			model.addAttribute("errorMessage", e.getMessage());

			// 再表示に必要なデータを再取得
			User staff = userRepository.findById(staffId).orElseThrow();
			model.addAttribute("staffShifts", shiftService.getShiftsByStaff(staff));
			model.addAttribute("staffId", staffId);

			return "staff_shift_management";
		}
	}

	// スタッフ自身のシフト削除（POST）
	@PostMapping("/shifts/{id}")
	public String deleteStaffShift(@PathVariable("id") Long shiftId) {
		shiftService.deleteShift(shiftId);
		return "redirect:/staff/shifts?success=shiftDeleted";
	}

}

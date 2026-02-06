//パッケージ宣言：予約に関する Web ルーティング一式
package com.example.twentysix.controller;

//予約日時の型（LocalDate/LocalTime）やコレクション
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

//リクエストパラメータの日付/時間文字列を Java 時間型に変換するためのアノテーション
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
//MVC コントローラ宣言
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
//テンプレートに値を受け渡すためのモデル
import org.springframework.ui.Model;
//ルーティング系アノテーション（GET/POST/パス変数など）
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.twentysix.entity.Coupon;
//予約エンティティ：フォームバインドや再表示で利用
import com.example.twentysix.entity.Reservation;
import com.example.twentysix.entity.SurveyResponse;
//ユーザエンティティ：顧客・スタッフの紐付けに使用
import com.example.twentysix.entity.User;
import com.example.twentysix.repository.CouponRepository;
//ユーザ検索のためのリポジトリ（メール→User、ID→User）
import com.example.twentysix.repository.UserRepository;
import com.example.twentysix.service.CouponService;
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
	private final CouponRepository couponRepository; // ★これを追加
	private final CouponService couponService;

	// 1. コンストラクタの引数に SurveyService surveyService を追加する
	public ReservationController(ReservationService reservationService,
			UserRepository userRepository,
			SurveyService surveyService, CouponRepository couponRepository, CouponService couponService) {

		this.reservationService = reservationService;
		this.userRepository = userRepository;

		// 2. 引数で受け取った surveyService をフィールドに代入する
		this.surveyService = surveyService;
		this.couponRepository = couponRepository;
		this.couponService = couponService;
	}

	// クーポンリポジトリをコンストラクタで注入しておいてください
	@PostMapping("/{id}/apply-coupon")
	@Transactional
	public String applyCoupon(@PathVariable("id") Long reservationId,
			@RequestParam("couponId") Long couponId,
			@AuthenticationPrincipal UserDetails userDetails) {

		// 1. 予約とクーポンを取得
		Reservation res = reservationService.getReservationById(reservationId)
				.orElseThrow(() -> new IllegalArgumentException("Invalid reservation Id"));
		Coupon coupon = couponRepository.findById(couponId)
				.orElseThrow(() -> new IllegalArgumentException("Invalid coupon Id"));

		// 2. セキュリティチェック（他人のクーポン利用防止）
		if (!coupon.getUser().getEmail().equals(userDetails.getUsername())) {
			return "redirect:/reservation/history?error=auth";
		}

		// 3. すでにクーポン適用済みでないかチェック
		if (res.getAppliedDiscount() > 0) {
			return "redirect:/reservation/history?error=already_applied";
		}

		// 4. 適用：クーポンを使用済みにし、予約に金額を反映
		res.setAppliedDiscount(coupon.getDiscountAmount());
		coupon.setUsed(true);

		// 5. 保存（@Transactionalにより自動でDBに反映されます）
		return "redirect:/reservation/history?success=couponApplied";
	}

	// 予約登録フォームの表示（空フォーム + スタッフ一覧）
	@GetMapping("/new")
	public String showReservationForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		// 1. ログイン中のユーザーを特定
		User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

		// 2. 未使用で有効なクーポンを取得してモデルに渡す（変数名は availableCoupons）
		model.addAttribute("availableCoupons", couponService.getAvailableCoupons(user));

		// 既存の処理
		model.addAttribute("staffs", reservationService.getAllStaffs());
		model.addAttribute("reservation", new Reservation());
		return "reservation_form";

	}

	// 予約履歴画面を表示
	@GetMapping("/history")
	public String showReservationHistory(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

		// 2. 履歴（削除済以外）を取得
		List<Reservation> history = reservationService.getUserReservations(user);
		model.addAttribute("userReservations", history);

		// ★追加：未使用で有効なクーポンリストを取得してモデルに渡す
		// CouponService を使って取得します
		model.addAttribute("availableCoupons", couponService.getAvailableCoupons(user));

		return "reservation_history";
	}

	// 予約作成の受付（POST）：顧客認証前提
	// 予約作成の受付（POST）：顧客認証前提
	@PostMapping("/new") // ★これが必要！
	@Transactional // ★クーポン消費と予約を同時に行うため推奨
	public String createReservation( // ★ここから引数が始まります
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam("staffId") Long staffId,
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam("timeSlot") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime timeSlot,
			@RequestParam("menu") String menu,
			@RequestParam(value = "couponId", required = false) Long couponId,
			Model model) { // ★中身はここから

		User customer = userRepository.findByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("Customer not found"));

		try {
			int discount = 0;
			// 1. クーポンが選択されている場合の消費処理
			if (couponId != null) {
				Coupon coupon = couponRepository.findById(couponId)
						.orElseThrow(() -> new IllegalArgumentException("Invalid coupon ID"));

				discount = coupon.getDiscountAmount();
				coupon.setUsed(true);
				couponRepository.save(coupon);
			}

			// 2. 予約作成（引数に discount を渡す）
			reservationService.createReservation(customer, staffId, date, timeSlot, menu, discount);

			return "redirect:/reservation/history?success=created";

		} catch (IllegalStateException e) {
			model.addAttribute("errorMessage", e.getMessage());
			model.addAttribute("staffs", reservationService.getAllStaffs());
			model.addAttribute("availableCoupons", couponService.getAvailableCoupons(customer));

			Reservation tempReservation = new Reservation();
			tempReservation.setStaff(userRepository.findById(staffId).orElse(null));
			tempReservation.setRecordDate(date);
			tempReservation.setTimeSlot(timeSlot);
			tempReservation.setMenu(menu);
			model.addAttribute("reservation", tempReservation);

			return "reservation_form";
		}
	}

	// 自分の予約履歴を一覧表示（ログインユーザに紐付く）

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
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam("staffRating") Integer staffRating,
			@RequestParam("serviceRating") Integer serviceRating,
			@RequestParam("comment") String comment, RedirectAttributes redirectAttributes,
			Model model) {
		try {
			boolean isWin = surveyService.saveSurveyResponse(
					userDetails.getUsername(), reservationId, staffRating, serviceRating, comment);

			// 今回のサイコロの結果（isWin）だけで判定する
			if (isWin) {
				redirectAttributes.addFlashAttribute("winMessage", "🎉 おめでとうございます！クーポンが当たりました！");
			} else {
				redirectAttributes.addFlashAttribute("loseMessage", "アンケートへのご協力ありがとうございました！");
			}

			return "redirect:/reservation/history?success=surveySubmitted";
		} catch (IllegalStateException | IllegalArgumentException e) {
			model.addAttribute("errorMessage", e.getMessage());
			model.addAttribute("reservation", reservationService.getReservationById(reservationId).get());
			model.addAttribute("surveyResponse", new SurveyResponse());
			return "survey_form";
		}
	}
}
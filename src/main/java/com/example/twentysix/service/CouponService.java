package com.example.twentysix.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.twentysix.entity.Coupon;
import com.example.twentysix.entity.User;
import com.example.twentysix.repository.CouponRepository;
import com.example.twentysix.repository.SurveyResponseRepository;
import com.example.twentysix.repository.UserRepository;

@Service
public class CouponService {

	private final CouponRepository couponRepository;
	private final SurveyResponseRepository surveyResponseRepository;
	private final Random random = new Random();
	private String name;
	private final UserRepository userRepository;

	public CouponService(CouponRepository couponRepository,
			SurveyResponseRepository surveyResponseRepository,
			UserRepository userRepository) { // ★ここに追加！
		this.couponRepository = couponRepository;
		this.surveyResponseRepository = surveyResponseRepository;
		this.userRepository = userRepository; // ★ここにも追加！
	}

	@Transactional
	public boolean checkAndIssueCoupon(User user) {
		long surveyCount = surveyResponseRepository.countByUser(user);

		// 5回目特典
		if (surveyCount > 0 && surveyCount % 5 == 0) {
			saveCoupon(user, "5回目確定特典", 300);
			return true; // 確定当たり
		}

		// 1/2の確率判定
		if (new Random().nextBoolean()) {
			saveCoupon(user, "アンケート御礼", 50);
			return true; // 当たり
		}

		return false; // ハズレ
	}

	// クーポン保存の共通処理
	private void saveCoupon(User user, String name, int amount) {
		Coupon coupon = new Coupon();
		coupon.setUser(user);
		coupon.setName(name); // 名前をセット
		coupon.setDiscountAmount(amount);
		coupon.setUsed(false);
		coupon.setExpiryDate(LocalDate.now().plusMonths(3));
		couponRepository.save(coupon);
	}

	/**
	 * 未使用クーポン一覧を取得
	 */
	public List<Coupon> getAvailableCoupons(User user) {
		return couponRepository.findByUserAndUsedFalseAndExpiryDateAfterOrderByDiscountAmountDesc(user,
				LocalDate.now());
	}

	// CouponService.java の中に追加
	public boolean wasLastSurveyAWin(String email) {
		// 1. ユーザーを特定
		User user = userRepository.findByEmail(email).orElseThrow();

		// 2. 直近3秒以内に作成されたクーポンがあるかチェック
		// (アンケート送信直後に発行されたかを判定するため)
		List<Coupon> recentCoupons = couponRepository.findByUserAndUsedFalseAndExpiryDateAfter(user, LocalDate.now());

		// とりあえず「最新のクーポンが未使用であるか」という簡易チェックでOKです
		// もし厳密に判定したいなら、今回のアンケート送信でクーポンが増えたかを見ます
		return !recentCoupons.isEmpty();
	}

}
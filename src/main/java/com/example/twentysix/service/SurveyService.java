package com.example.twentysix.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.twentysix.entity.SurveyResponse;
import com.example.twentysix.entity.User; // 追加
import com.example.twentysix.repository.ReservationRepository;
import com.example.twentysix.repository.SurveyResponseRepository;
import com.example.twentysix.repository.UserRepository; // 追加

@Service
public class SurveyService {
	private final SurveyResponseRepository surveyResponseRepository;
	private final ReservationRepository reservationRepository;
	private final UserRepository userRepository; // 追加
	private final CouponService couponService; // 追加

	// コンストラクタに userRepository と couponService を追加
	public SurveyService(SurveyResponseRepository surveyResponseRepository,
			ReservationRepository reservationRepository,
			UserRepository userRepository,
			CouponService couponService) {
		this.surveyResponseRepository = surveyResponseRepository;
		this.reservationRepository = reservationRepository;
		this.userRepository = userRepository;
		this.couponService = couponService;
	}

	@Transactional
	public boolean saveSurveyResponse(String email, Long reservationId, int staffRating, int serviceRating,
			String comment) {

		// メールアドレスからユーザーを特定
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("User not found"));

		// 保存処理
		SurveyResponse response = new SurveyResponse();
		response.setUser(user);
		response.setReservation(reservationRepository.findById(reservationId)
				.orElseThrow(() -> new RuntimeException("Reservation not found")));
		response.setStaffRating(staffRating);
		response.setServiceRating(serviceRating);
		response.setComment(comment);

		surveyResponseRepository.save(response);

		// ★ついにクーポン判定を呼び出す！
		return couponService.checkAndIssueCoupon(user);
	}
}
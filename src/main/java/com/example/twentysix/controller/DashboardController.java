package com.example.twentysix.controller;

import java.time.LocalDate;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.twentysix.entity.User;
import com.example.twentysix.repository.ReservationRepository;
import com.example.twentysix.repository.SurveyResponseRepository; // 追加
import com.example.twentysix.repository.UserRepository;

@Controller
public class DashboardController { // ← クラス名の宣言を復活

	// フィールド宣言をここにまとめる
	private final UserRepository userRepository;
	private final ReservationRepository reservationRepository;
	private final SurveyResponseRepository surveyResponseRepository; // 追加

	// コンストラクタ（3つのリポジトリを DI する）
	public DashboardController(UserRepository userRepository,
			ReservationRepository reservationRepository,
			SurveyResponseRepository surveyResponseRepository) {
		this.userRepository = userRepository;
		this.reservationRepository = reservationRepository;
		this.surveyResponseRepository = surveyResponseRepository;
	}

	@GetMapping("/dashboard")
	public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		// 1. ログインユーザーを取得
		User currentUser = userRepository.findByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		// 2. 管理者（ADMIN）の場合
		if ("ROLE_ADMIN".equals(currentUser.getRole())) {
			model.addAttribute("recentReservations",
					reservationRepository.findByRecordDateBetween(LocalDate.now().minusDays(7),
							LocalDate.now().plusDays(7)));

			// 最新のアンケート回答を取得して渡す
			model.addAttribute("latestSurveys", surveyResponseRepository.findAllByOrderByIdDesc());

			return "admin_dashboard";
		}

		// 3. スタッフ（STAFF）の場合
		else if ("ROLE_STAFF".equals(currentUser.getRole())) {
			model.addAttribute("todayReservations",
					reservationRepository.findByStaffAndRecordDateBetween(currentUser, LocalDate.now(),
							LocalDate.now()));
			return "staff_dashboard";
		}

		// 4. 顧客（CUSTOMER）の場合
		else {
			model.addAttribute("userReservations",
					reservationRepository.findByUserOrderByRecordDateDescTimeSlotDesc(currentUser));
			return "customer_dashboard";
		}
	}
}
package com.example.twentysix.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.twentysix.entity.Coupon;
import com.example.twentysix.entity.User;
import com.example.twentysix.repository.UserRepository;
import com.example.twentysix.service.CouponService;

@Controller
@RequestMapping("/coupons")
public class CouponController {

	private final CouponService couponService;
	private final UserRepository userRepository;

	public CouponController(CouponService couponService, UserRepository userRepository) {
		this.couponService = couponService;
		this.userRepository = userRepository;
	}

	@GetMapping
	public String viewMyCoupons(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		// 1. ログイン中のユーザーを取得
		User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

		// 2. 未使用クーポンを取得
		List<Coupon> allCoupons = couponService.getAvailableCoupons(user);

		// 3. クーポンを種類（金額）ごとに分類して Model に渡す
		model.addAttribute("coupons300", allCoupons.stream()
				.filter(c -> c.getDiscountAmount() == 300).collect(Collectors.toList()));

		model.addAttribute("coupons50", allCoupons.stream()
				.filter(c -> c.getDiscountAmount() == 50).collect(Collectors.toList()));

		return "coupons";
	}
}
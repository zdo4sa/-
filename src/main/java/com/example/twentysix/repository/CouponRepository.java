package com.example.twentysix.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.twentysix.entity.Coupon;
import com.example.twentysix.entity.User;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
	// 未使用、かつ有効期限が今日以降のクーポンを、金額の高い順に取得

	// 1. 未使用かつ有効期限内のクーポンを、割引額が大きい順に取得
	List<Coupon> findByUserAndUsedFalseAndExpiryDateAfterOrderByDiscountAmountDesc(User user, LocalDate date);

	// 2. 赤線を消すためのメソッド：未使用かつ有効期限内のクーポンがあるかチェック
	List<Coupon> findByUserAndUsedFalseAndExpiryDateAfter(User user, LocalDate date);

	// 3. 顧客が持っている未使用クーポンをすべて取得（予約フォーム用）
	List<Coupon> findByUserAndUsedFalse(User user);
}
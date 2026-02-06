package com.example.twentysix.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Data; // もし Lombok を使っているならこれだけでOK

@Entity
@Table(name = "coupons")
@Data
public class Coupon {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	// Coupon.java 内に追加
	private String name;

	// user に対する Getter
	public User getUser() {
		return this.user;
	}

	// used に対する Getter (boolean型は get ではなく is を使うのが一般的です)
	public boolean isUsed() {
		return this.used;
	}

	// discountAmount に対する Getter
	public int getDiscountAmount() {
		return this.discountAmount;
	}

	// used を更新するための Setter も必要です
	public void setUsed(boolean used) {
		this.used = used;
	}

	// expiryDate をセットするための Setter (Serviceで使用します)
	public void setExpiryDate(LocalDate expiryDate) {
		this.expiryDate = expiryDate;
	}

	// 必要に応じて user と discountAmount の Setter も
	public void setUser(User user) {
		this.user = user;
	}

	public void setDiscountAmount(int discountAmount) {
		this.discountAmount = discountAmount;
	}

	public String getName() {
		return name;
	}

	// 3. name 用の Setter を追加
	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;

	private int discountAmount; // 50 または 300
	private boolean used = false; // 使用済みフラグ
	private LocalDate expiryDate; // 有効期限

}

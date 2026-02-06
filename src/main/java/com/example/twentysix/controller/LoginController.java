////MVC コントローラ宣言（ログイン画面の GET 表示のみ担当）
//package com.example.twentysix.controller;
//
////画面遷移を担うコントローラであることを示す
//import org.springframework.stereotype.Controller;
////GET マッピング用アノテーション（/login へのアクセスを画面に紐づけ）
//import org.springframework.web.bind.annotation.GetMapping;
//
////コントローラクラス定義
//@Controller
//public class LoginController {
//	//ログインページの表示（SecurityConfig で loginPage("/login") と対応）
//	@GetMapping("/login")
//	public String login() {
//		//resources/templates/login.html を返す
//		return "login";
//	}
//}
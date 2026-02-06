package com.example.twentysix.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.twentysix.entity.User;
import com.example.twentysix.repository.UserRepository;

@Controller
public class AuthController {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping("/login")
	public String login() {
		return "login";
	}

	@GetMapping("/register")
	public String showRegistrationForm() {
		return "register";
	}

	@PostMapping("/register")
	public String registerUser(@RequestParam("name") String name,
			@RequestParam("email") String email,
			@RequestParam("password") String password,
			Model model) {

		// 1. メールアドレスの重複チェック
		if (userRepository.findByEmail(email).isPresent()) {
			model.addAttribute("errorMessage", "このメールアドレスは既に登録されています。");
			return "register";
		}

		// 2. 新しいユーザーを作成
		User user = new User();
		user.setName(name);
		user.setEmail(email);
		user.setRole("CUSTOMER"); // 一般顧客として登録

		// 3. ★ハッシュ化（魔法）をかける：生のパスワードを暗号文 "$2a$10$..." に変換
		String encodedPassword = passwordEncoder.encode(password);
		user.setPassword(encodedPassword);

		// 4. 保存
		userRepository.save(user);

		// 5. ログイン画面へリダイレクト
		return "redirect:/login?register_success";
	}
}
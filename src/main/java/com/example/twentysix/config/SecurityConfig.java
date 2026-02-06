package com.example.twentysix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.twentysix.repository.UserRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(authorize -> authorize
						// ログイン、会員登録、静的ファイルは全員許可
						.requestMatchers("/login", "/register/**", "/css/**", "/js/**").permitAll()
						// 権限別の制限
						.requestMatchers("/admin/**").hasRole("ADMIN")
						.requestMatchers("/staff/**").hasAnyRole("STAFF", "ADMIN")
						.anyRequest().authenticated())
				.formLogin(form -> form
						.loginPage("/login")
						.defaultSuccessUrl("/dashboard", true)
						.permitAll())
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/login?logout")
						.permitAll());
		return http.build();
	}

	@Bean
	public UserDetailsService userDetailsService(UserRepository userRepository) {
		return email -> userRepository.findByEmail(email)
				.map(user -> org.springframework.security.core.userdetails.User.builder()
						.username(user.getEmail())
						.password(user.getPassword())
						.authorities(user.getRole())
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
	}

	// ★重要：ここを「passwordEncoder」ひとつだけに統一してください
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
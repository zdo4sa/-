// パッケージ宣言：このコントローラの論理的な配置先
package com.example.twentysix.controller;

// 今日・相対期間の計算に使用する日付 API
import java.time.LocalDate;

// 認証済みユーザの principal（UserDetails）を引き当てるためのアノテーション
import org.springframework.security.core.annotation.AuthenticationPrincipal;
// Spring Security が扱うユーザ情報の標準インタフェース
import org.springframework.security.core.userdetails.UserDetails;
// MVC のコントローラであることを示す
import org.springframework.stereotype.Controller;
// テンプレートに値を渡すためのモデル
import org.springframework.ui.Model;
// GET リクエストのハンドラマッピング
import org.springframework.web.bind.annotation.GetMapping;

import com.example.twentysix.entity.User;
// 予約検索のための JPA リポジトリ（ダッシュボード表示データ取得に使用）
import com.example.twentysix.repository.ReservationRepository;
// ユーザ検索のための JPA リポジトリ（メールで User を引く）
import com.example.twentysix.repository.UserRepository;

// このクラスが MVC コントローラであることを表明
@Controller
public class DashboardController {
	// ユーザ情報へアクセスするためのリポジトリ（メールから User を取得）
	private final UserRepository userRepository;
	// 予約情報へアクセスするためのリポジトリ（役割別に表示内容を切り替える）
	private final ReservationRepository reservationRepository;

	// 依存コンポーネント（リポジトリ）をコンストラクタで受け取り、DI する
	public DashboardController(UserRepository userRepository, ReservationRepository reservationRepository) {
		// フィールド userRepository に代入
		this.userRepository = userRepository;
		// フィールド reservationRepository に代入
		this.reservationRepository = reservationRepository;
	}

	//ダッシュボードのルート。ログイン後の遷移先
	@GetMapping("/dashboard")
	public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		//ログイン名（=メール）からユーザエンティティを取得。見つからない場合はランタイム例外
		User currentUser = userRepository.findByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		//役割が ADMIN の場合の表示：最近の予約（±7 日）を一覧表示
		if (currentUser.getRole().equals("ADMIN")) {
			//期間：今日-7 日〜今日+7 日で予約を取得
			model.addAttribute("recentReservations",
					reservationRepository.findByRecordDateBetween(LocalDate.now().minusDays(7),
							LocalDate.now().plusDays(7)));
			//管理者ダッシュボードテンプレートへ
			return "admin_dashboard";

			//役割が STAFF の場合：本日の自担当予約のみを表示
		} else if (currentUser.getRole().equals("STAFF")) {
			//当日 1 日分（start=end）でスタッフの予約を抽出
			model.addAttribute("todayReservations", reservationRepository.findByStaffAndRecordDateBetween(currentUser,
					LocalDate.now(), LocalDate.now()));
			//スタッフダッシュボードテンプレートへ
			return "staff_dashboard";

		} else { // CUSTOMER の場合：自分の予約履歴（最新→過去）を表示
			//自ユーザの予約を日付降順・時間降順で取得し画面へ
			model.addAttribute("userReservations",
					reservationRepository.findByUserOrderByRecordDateDescTimeSlotDesc(currentUser));
			//顧客ダッシュボードテンプレートへ
			return "customer_dashboard";

		}
	}
}
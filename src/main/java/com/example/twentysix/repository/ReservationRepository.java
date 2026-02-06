// このインターフェースが属するパッケージ
package com.example.twentysix.repository;

// 日付・時間での検索に使う型
import java.time.LocalDate;
import java.time.LocalTime;
// 結果が 0 or 1 件のときに便利な Optional
import java.util.List;
import java.util.Optional;

// Spring Data JPA のリポジトリ基底インターフェース
import org.springframework.data.jpa.repository.JpaRepository;
// Spring のステレオタイプ（コンポーネントスキャン対象にする）
import org.springframework.stereotype.Repository;

// 予約エンティティを扱うためのインポート
import com.example.twentysix.entity.Reservation;
// ユーザ（顧客/スタッフ）で絞り込むためのインポート
import com.example.twentysix.entity.User;

// このインターフェースがリポジトリ層の Bean であることを明示
@Repository
// Reservation エンティティ用の CRUD + クエリメソッド定義
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
	// 指定ユーザの予約を「日付降順→時間降順」で取得（履歴表示に使用）
	List<Reservation> findByUserOrderByRecordDateDescTimeSlotDesc(User user);

	// 指定スタッフ担当の予約を「日付降順→時間降順」で取得（スタッフ画面用）
	List<Reservation> findByStaffOrderByRecordDateDescTimeSlotDesc(User staff);

	// 同一スタッフ・同一日付・同一時間の予約があるかを確認（重複予約防止）
	Optional<Reservation> findByRecordDateAndTimeSlotAndStaff(LocalDate date, LocalTime timeSlot, User staff);

	// 期間で予約を抽出（管理者の最近予約や統計で使用）
	List<Reservation> findByRecordDateBetween(LocalDate startDate, LocalDate endDate);

	// スタッフを縛って、特定日範囲の予約を抽出（本日の予約など）
	List<Reservation> findByStaffAndRecordDateBetween(User staff, LocalDate startDate, LocalDate endDate);

	// 指定したステータス「以外」をすべて取得する
	List<Reservation> findByStatusNot(String status);

	// 期間指定かつ、指定したステータス「以外」を取得する
	List<Reservation> findByRecordDateBetweenAndStatusNot(LocalDate start, LocalDate end, String status);

	List<Reservation> findByUserAndStatusNotOrderByRecordDateDescTimeSlotDesc(User user, String status);

	List<Reservation> findByStaffAndRecordDateAndStatusNot(User staff, LocalDate date, String status);
}
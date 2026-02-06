// サービスクラスのパッケージ配置
package com.example.twentysix.service;

// 日付・時刻 API（LocalDate/LocalTime）
import java.time.LocalDate;
import java.time.LocalTime;
// 可変長のスロット生成やフィルタに使用するコレクション
import java.util.ArrayList;
import java.util.List;
// 統計返却用の Map など
import java.util.Map;
// 存在しない可能性のある値を安全に扱うコンテナ
import java.util.Optional;
// 集約やフィルタのための Stream 操作
import java.util.stream.Collectors;

// サービス層のステレオタイプ（DI 管理対象）
import org.springframework.stereotype.Service;
// トランザクション境界の宣言（同一メソッド内を 1 トランザクションに）
import org.springframework.transaction.annotation.Transactional;

// 予約エンティティの参照（作成/更新/返却）
import com.example.twentysix.entity.Reservation;
// ユーザエンティティ（顧客・スタッフの特定）
import com.example.twentysix.entity.User;
// 予約テーブルへの永続化・検索を担う JPA リポジトリ
import com.example.twentysix.repository.ReservationRepository;
// シフトテーブルへのアクセス（空き判定に必須）
import com.example.twentysix.repository.ShiftRepository;
// ユーザテーブルへのアクセス（ID/メール→User 解決）
import com.example.twentysix.repository.UserRepository;

// 業務ロジックをまとめるサービスクラス
@Service
public class ReservationService {
	// 予約の CRUD・クエリを扱うリポジトリ
	private final ReservationRepository reservationRepository;
	// ユーザ解決（顧客/スタッフ）に使用
	private final UserRepository userRepository;
	// シフト有無・時間内判定のために参照
	private final ShiftRepository shiftRepository;

	//依存性のコンストラクタ注入（テスト容易性と不変性のため final）
	public ReservationService(ReservationRepository reservationRepository, UserRepository userRepository,
			ShiftRepository shiftRepository) {
		//フィールドへ予約リポジトリを設定
		this.reservationRepository = reservationRepository;
		//フィールドへユーザリポジトリを設定
		this.userRepository = userRepository;
		//フィールドへシフトリポジトリを設定
		this.shiftRepository = shiftRepository;

	}

	//指定ユーザの予約履歴（新しい順）を取得
	public List<Reservation> getUserReservations(User user) {
		// 顧客の履歴からも「削除済」を除外して、新しい順に表示する
		return reservationRepository.findByUserAndStatusNotOrderByRecordDateDescTimeSlotDesc(user, "削除済");
	}

	//予約を ID で 1 件取得（存在しなければ Optional.empty）
	public Optional<Reservation> getReservationById(Long id) {
		//JPA の findById を委譲
		return reservationRepository.findById(id);
	}

	//全予約の一覧を取得（管理者用）
	public List<Reservation> getAllReservations() {
		// すべて取得する代わりに「削除済」以外を取得するように変更
		return reservationRepository.findByStatusNot("削除済");
	}

	//期間指定で予約を抽出（統計・フィルタ表示用）
	// 期間指定での予約取得（削除済を除外）
	public List<Reservation> getReservationsByDateRange(LocalDate start, LocalDate end) {
		// 期間内かつ「削除済」以外を取得するように変更
		return reservationRepository.findByRecordDateBetweenAndStatusNot(start, end, "削除済");
	}

	//予約更新（別スロットへの変更時も競合/シフト内を厳密チェック）
	@Transactional
	public Reservation updateReservation(Long reservationId, LocalDate newDate, LocalTime newTimeSlot,
			String newMenu) {
		//対象予約を ID で取得（なければ 400 相当の業務例外）
		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
		//変更先スロットが、同じスタッフ・同じ日・同じ時間の他予約と衝突しないか
		if (reservationRepository.findByRecordDateAndTimeSlotAndStaff(newDate, newTimeSlot,
				reservation.getStaff())
				.filter(r -> !r.getId().equals(reservationId)) // 自分自身なら許容
				.isPresent()) {
			//競合ありの場合は業務例外
			throw new IllegalStateException("This new time slot is already booked.");
		}
		//変更先がスタッフのシフト時間内かをチェック

		boolean staffHasShift = shiftRepository.findByStaffAndRecordDate(reservation.getStaff(), newDate)
				.map(shift -> !newTimeSlot.isBefore(shift.getStartTime())
						&& !newTimeSlot.isAfter(shift.getEndTime().minusMinutes(1))) // 上限未満チェック
				.orElse(false);
		//シフト外なら更新不可
		if (!staffHasShift) {
			//利用不可メッセージで業務例外
			throw new IllegalStateException("Staff is not available at this new time.");
		}
		//問題なければ、日付・時間・メニューを更新
		reservation.setRecordDate(newDate);
		reservation.setTimeSlot(newTimeSlot);
		reservation.setMenu(newMenu);
		//保存して最新状態を返す
		return reservationRepository.save(reservation);
	}

	//予約キャンセル（物理削除はせずステータス更新）
	@Transactional
	public void cancelReservation(Long reservationId) {
		System.out.println("DEBUG: 予約保存メソッドが呼ばれました");
		//対象予約を ID で取得
		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
		//ステータスを「キャンセル済」に変更
		reservation.setStatus("キャンセル済");
		//上書き保存
		reservationRepository.save(reservation);
	}

	//スタッフ一覧（ロール=STAFF のみ）を取得
	public List<User> getAllStaffs() {
		//ユーザテーブルから "STAFF" ロールのユーザを返す
		return userRepository.findByRole("ROLE_STAFF");
	}

	//指定スタッフ・日付の空き時間枠一覧を計算して返す（30 分刻み）
	public List<LocalTime> getAvailableTimeSlots(Long staffId, LocalDate date) {
		User staff = userRepository.findById(staffId)
				.orElseThrow(() -> new IllegalArgumentException("Staff not found"));

		// 1. 「キャンセル済」を除外した予約リストを取得
		List<Reservation> activeReservations = reservationRepository.findByStaffAndRecordDateAndStatusNot(
				staff, date, "キャンセル済");

		// 2. シフトを取得（Optional のまま受け取る）
		Optional staffShiftOpt = shiftRepository.findByStaffAndRecordDate(staff, date);

		if (staffShiftOpt.isEmpty()) {
			return List.of();
		}

		// 3. 箱から取り出し、Shift型として扱う（これで赤線が消えます）
		com.example.twentysix.entity.Shift shift = (com.example.twentysix.entity.Shift) staffShiftOpt.get();

		LocalTime shiftStart = shift.getStartTime();
		LocalTime shiftEnd = shift.getEndTime();

		// 4. 30分刻みの全スロット作成
		List<LocalTime> allPossibleSlots = generateTimeSlots(shiftStart, shiftEnd, 30);

		// 5. 有効な予約（activeReservations）と重ならない枠だけを返す
		return allPossibleSlots.stream()
				.filter(slot -> activeReservations.stream().noneMatch(res -> res.getTimeSlot().equals(slot)))
				.collect(Collectors.toList());
	}

	//開始時刻から終了時刻未満まで、指定分刻みで LocalTime のリストを生成
	// 開始時刻から終了時刻未満まで、指定分刻みで LocalTime のリストを生成
	private List<LocalTime> generateTimeSlots(LocalTime start, LocalTime end, int intervalMinutes) {
		List<LocalTime> slots = new ArrayList<>();

		// 安全策：間隔が0以下なら即座に空リストを返す（無限ループ防止）
		if (intervalMinutes <= 0 || start == null || end == null) {
			return slots;
		}

		LocalTime current = start;

		// 終了時刻「未満」までループ
		while (current.isBefore(end)) {
			slots.add(current);

			// ★重要：ここで時間を進める。これがないと無限ループになります
			LocalTime next = current.plusMinutes(intervalMinutes);

			// 万が一、24時間を回って戻ってしまった場合の無限ループ防止
			if (next.isBefore(current)) {
				break;
			}
			current = next;
		}
		return slots;
	}

	// 期間内の予約をメニュー名で集計し、件数マップを返す
	public Map<String, Long> getReservationCountByMenu(LocalDate startDate, LocalDate endDate) {
		List<Reservation> reservations = reservationRepository.findByRecordDateBetween(startDate, endDate);
		return reservations.stream()
				// ★追加：キャンセル済を除外
				.filter(r -> !"キャンセル済".equals(r.getStatus()))
				.collect(Collectors.groupingBy(Reservation::getMenu, Collectors.counting()));
	}

	// 期間内の予約をスタッフ名で集計（null スタッフを除外）
	public Map<String, Long> getReservationCountByStaff(LocalDate startDate, LocalDate endDate) {
		List<Reservation> reservations = reservationRepository.findByRecordDateBetween(startDate, endDate);
		return reservations.stream()
				.filter(r -> r.getStaff() != null)
				// ★追加：キャンセル済を除外
				.filter(r -> !"キャンセル済".equals(r.getStatus()))
				.collect(Collectors.groupingBy(r -> r.getStaff().getName(), Collectors.counting()));
	}

	// ReservationService.java
	@Transactional
	public void createReservation(User user, Long staffId, LocalDate date, LocalTime time, String menu, int discount) { // ★最後に int discount を追加
		// 1. スタッフを取得
		User staff = userRepository.findById(staffId)
				.orElseThrow(() -> new IllegalArgumentException("Staff not found"));

		// 2. 予約エンティティを作成
		Reservation reservation = new Reservation();
		reservation.setUser(user);
		reservation.setStaff(staff);
		reservation.setRecordDate(date);
		reservation.setTimeSlot(time);
		reservation.setMenu(menu);
		reservation.setStatus("予約済");

		// 3. ★重要：割引額をセットする
		reservation.setAppliedDiscount(discount);

		// 4. 保存
		reservationRepository.save(reservation);
	}

	@Transactional // ← これが非常に重要です
	public void deleteReservation(Long id) {
		// 1. 予約データを取得
		Reservation reservation = reservationRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("予約が見つかりません: " + id));

		// 2. 【物理削除はやめる】 DBから消さずに、ステータスを「削除済」に更新する
		reservation.setStatus("削除済");

		// 3. 保存（これで一覧には「削除済」として残るが、DBからは消えない）
		reservationRepository.save(reservation);

	}
}
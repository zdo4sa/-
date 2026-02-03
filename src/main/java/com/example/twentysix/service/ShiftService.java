// サービスクラスのパッケージ
package com.example.twentysix.service;

// 日付・時刻・コレクション
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

// サービス層のステレオタイプ
import org.springframework.stereotype.Service;
// トランザクション管理（更新系で使用）
import org.springframework.transaction.annotation.Transactional;

// シフト・ユーザ各エンティティの参照
import com.example.twentysix.entity.Shift;
import com.example.twentysix.entity.User;
import com.example.twentysix.repository.ShiftRepository;
import com.example.twentysix.repository.UserRepository;

// シフト領域の業務ロジックを担当するサービス
@Service
public class ShiftService {
	// シフトテーブルへのアクセス窓口
	private final ShiftRepository shiftRepository;
	// ユーザテーブルへのアクセス窓口（staffId→User 取得など）
	private final UserRepository userRepository;

	// 依存性のコンストラクタ注入
	public ShiftService(ShiftRepository shiftRepository, UserRepository userRepository) {
		// フィールドへシフトリポジトリ設定
		this.shiftRepository = shiftRepository;
		// フィールドへユーザリポジトリ設定
		this.userRepository = userRepository;
	}

	public List<Shift> getStaffShifts(User staff) {
		// 派生クエリ findByStaffOrderByDateAscStartTimeAsc を利用
		return shiftRepository.findByStaffOrderByRecordDateAscStartTimeAsc(staff);
	}

	//シフトを ID で 1 件取得（編集前の存在確認などに使用）
	public Optional<Shift> getShiftById(Long id) {
		//JPA の findById を委譲
		return shiftRepository.findById(id);
	}

	//シフトを作成または更新（同日シフトがあれば上書き）
	@Transactional
	public Shift createOrUpdateShift(Long staffId, LocalDate date, LocalTime startTime, LocalTime endTime) {
		//staffId から User を取得（なければ 400 相当の業務例外）
		if (!startTime.isBefore(endTime)) {
			throw new IllegalArgumentException("終了時間は開始時間よりも後の時刻を指定してください。");
		}
		User staff = userRepository.findById(staffId)
				.orElseThrow(() -> new IllegalArgumentException("Staff not found"));
		//既存シフトの有無を確認（staff+date のユニーク性を業務で担保）
		Optional<Shift> existingShift = shiftRepository.findByStaffAndRecordDate(staff, date);
		//更新対象となる Shift の実体（既存 or 新規）
		Shift shift;
		//既存があればフィールドを上書き
		if (existingShift.isPresent()) {
			//既存エンティティを取得
			shift = existingShift.get();
			//開始時間を更新
			shift.setStartTime(startTime);
			//終了時間を更新
			shift.setEndTime(endTime);
			//既存がなければ新規作成
		} else {
			//新しいシフトエンティティを作成
			shift = new Shift();
			//スタッフを紐付け
			shift.setStaff(staff);
			//日付を設定
			shift.setRecordDate(date);
			//開始時間を設定
			shift.setStartTime(startTime);
			//終了時間を設定
			shift.setEndTime(endTime);
		}
		//保存して永続化（新規は INSERT、既存は UPDATE）

		return shiftRepository.save(shift);
	}

	//シフトの削除（物理削除）
	@Transactional
	public void deleteShift(Long shiftId) {
		//主キー指定で削除（存在しない場合は例外なく no-op だが、整合性のため存在確認する実装も可）
		shiftRepository.deleteById(shiftId);
	}

	//全シフトの一覧を取得（管理者用）
	public List<Shift> getAllShifts() {
		//shift テーブルの全件を返す
		return shiftRepository.findAll();
	}

	//期間指定でシフトを抽出（管理者のフィルタ表示用）
	public List<Shift> getShiftsByDateRange(LocalDate startDate, LocalDate endDate) {
		//startDate <= record_date <= endDate の範囲で抽出
		return shiftRepository.findByRecordDateBetween(startDate, endDate);
	}

	public List<Shift> getShiftsByStaff(User staff) {
		// リポジトリのメソッドを呼び出す（後述のリポジトリ修正も必要です）
		return shiftRepository.findByStaffOrderByRecordDateDesc(staff);
	}
}
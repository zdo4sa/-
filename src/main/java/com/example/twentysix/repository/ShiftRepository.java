package com.example.twentysix.repository;

//日付での検索に使う型
import java.time.LocalDate;
//コレクション/Optional
import java.util.List;
import java.util.Optional;

//Spring Data JPA の基底インターフェース
import org.springframework.data.jpa.repository.JpaRepository;
//リポジトリのステレオタイプ
import org.springframework.stereotype.Repository;

//パッケージ宣言：シフト関連の永続化インターフェース置き場
//シフト・ユーザ各エンティティのインポート
import com.example.twentysix.entity.Shift;
import com.example.twentysix.entity.User;

//リポジトリ Bean であることを明示
@Repository
//Shift エンティティの CRUD + 派生クエリ
public interface ShiftRepository extends JpaRepository<Shift, Long> {
	//指定スタッフのシフトを「日付昇順→開始時刻昇順」で取得（見通しの良い並び）

	List<Shift> findByStaffOrderByRecordDateAscStartTimeAsc(User staff);

	//指定スタッフ・指定日のシフトを 1 件取得（存在すれば更新、なければ作成の判定に使用）
	Optional<Shift> findByStaffAndRecordDate(User staff, LocalDate recordDate);

	//期間でシフトを抽出（管理者の全体ビューやフィルタに使用）

	List<Shift> findByRecordDateBetween(LocalDate start, LocalDate end);

	List<Shift> findByStaffOrderByRecordDateDesc(User staff);
}

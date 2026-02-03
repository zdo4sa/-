package com.example.twentysix.repository;

//コレクション/Optional
import java.util.List;
import java.util.Optional;

//Spring Data JPA の基底インターフェース
import org.springframework.data.jpa.repository.JpaRepository;
//リポジトリのステレオタイプ
import org.springframework.stereotype.Repository;

//パッケージ宣言：ユーザ関連の永続化インターフェース置き場
//ユーザエンティティのインポート
import com.example.twentysix.entity.User;

//リポジトリ Bean であることを明示
@Repository
//User エンティティの CRUD + ログイン/検索用メソッド
public interface UserRepository extends JpaRepository<User, Long> {
	//メールアドレスでユーザを 1 件取得（認証時の検索に使用）
	Optional<User> findByEmail(String email);

	//ロール名でユーザ一覧を取得（STAFF 一覧のプルダウン等に使用）
	List<User> findByRole(String role);
}
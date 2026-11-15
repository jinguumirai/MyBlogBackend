# MyBlog
This is a simple blog backend project.

## JWT検証の共通化 (CommonLib)

トークンの検証ロジックは `CommonLib` の `net.myBlog.common.jwt.JwtTokenVerifier` に集約している。
生成(署名)は認証サービス(Login)の `net.myblog.login.service.AuthService` だけが行い、
検証は全マイクロサービスが共通ライブラリを利用して行う。

### 他のサービスでの利用手順

1. `pom.xml` に CommonLib を依存追加する

```xml
<dependency>
    <groupId>net.myblog</groupId>
    <artifactId>CommonLib</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

2. 検証用の公開鍵のパスを設定する(秘密鍵は配布しない)

```yaml
jwt:
  public-key-path: classpath:pems/prod_public_key.pem   # 未設定のサービスのみBeanが生成されない
  # enabled: false                                        # 明示的に無効化したい場合
```

3. Beanをインジェクションして使用する

```java
public String currentUser(String token) {
    if (!jwtTokenVerifier.validateToken(token)) {
        throw new SystemException(...);
    }
    return jwtTokenVerifier.extractSubject(token);
}
```

### 仕組み

| クラス | 役割 |
| --- | --- |
| `net.myBlog.common.jwt.JwtTokenVerifier` | 公開鍵による署名・有効期限の検証、subject/email/有効期限の取得 |
| `net.myBlog.common.jwt.PemKeyLoader` | PEM形式の公開鍵/秘密鍵の読み込み(ヘッダー除去・DERデコード) |
| `net.myBlog.common.properties.JwtProperties` | `jwt.public-key-path` / `jwt.enabled` の設定バインド |
| `net.myBlog.common.configs.JwtAutoConfiguration` | 上記を自動構成(`jwt.public-key-path` が設定されたサービスのみ有効) |

- `jwt.public-key-path` を設定していないサービスにはBeanが生成されないため、既存サービスへ影響しない。
- 鍵の読み込みは起動時に行うため、パスや形式の誤りは起動時に検知できる。
- リクエスト単位の検証はHTTP呼び出しではなくプロセス内で完結する(認証サービスへの問い合わせは不要)。

## 監査項目の自動補完 (CommonLib)

`net.myBlog.common.BasicEntity` を継承したエンティティは、INSERT/UPDATE時に
登録者・登録日時・更新者・更新日時が自動で補完される。
補完の実現方法はデータアクセス方式によって異なるが、補完内容と上書き規則は共通である。

| データアクセス | 補完の実現方法 | MyBatis-Plusの要否 |
| --- | --- | --- |
| MyBatis / MyBatis-Plus | `AuditInterceptor`(MyBatisのインターセプタ) | **不要**(MyBatis本体のみ) |
| JPA/Hibernate | `BasicEntity` の `@PrePersist`/`@PreUpdate` | 不要 |

| 列 | プロパティ | INSERT | UPDATE |
| --- | --- | --- | --- |
| `create_user_id` | `createUserId` | 補完(明示設定済みなら上書きしない) | - |
| `create_time` | `createTime` | 補完(明示設定済みなら上書きしない) | - |
| `update_user_id` | `updateUserId` | 補完(明示設定済みなら上書きしない) | 常に最新の操作者へ上書き |
| `update_time` | `updateTime` | 補完(明示設定済みなら上書きしない) | 常に最新日時へ上書き |

CommonLibは特定のO/Rマッパーに依存しない(`org.mybatis:mybatis` のみ任意依存)。
`BasicEntity` にもMyBatis-Plusのアノテーションは付けていないため、
MyBatis-Plusを使うサービスでも、プレーンMyBatis(XMLマッパー)のサービスでも、
JPAのサービスでも同じように監査項目が補完される。

### 使い方(MyBatis／MyBatis-Plusを使うサービス)

1. `pom.xml` に CommonLib を依存追加する(MyBatisまたはMyBatis-Plusも依存に含めること)

```xml
<dependency>
    <groupId>net.myblog</groupId>
    <artifactId>CommonLib</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

2. エンティティで `BasicEntity` を継承する(監査項目は基底クラスで定義済み)

```java
@Entity                 // JPAを使う場合
@TableName("article")   // MyBatis-Plusを使う場合は省略可(クラス名からarticleを導出)
public class Article extends BasicEntity {
    private String title;
    // getter/setter
}
```

3. Mapperのinsert/updateを呼ぶだけで監査項目が設定される

```java
articleMapper.insert(article);   // create_user_id / create_time / update_user_id / update_time を自動設定
```

4. MyBatis-Plusを使う場合は、主キーの採番方法をDBのAUTO_INCREMENTに設定する
   (`BasicEntity` にMyBatis-Plusの `@TableId` を付けないため。未設定だとスノーフローIDが採番される)

```yaml
mybatis-plus:
  global-config:
    db-config:
      id-type: auto
```

5. 登録者IDはSecurityContextのprincipal(JWTのsubject)から取得する。
   認証フィルタを導入していないサービスでは匿名ユーザーID(既定は0)が設定される。

```yaml
common:
  audit:
    enabled: true            # false で自動補完を無効化
    anonymous-user-id: 0     # ユーザーを特定できない場合に設定するID
```

取得方法をサービス固有にしたい場合は `CurrentUserIdProvider` の実装をBean定義すれば、
既定の実装は無効になる(`@ConditionalOnMissingBean`)。

### 使い方(JPAを使うサービス・MyBatis不要)

1. `pom.xml` に CommonLib を依存追加する

```xml
<dependency>
    <groupId>net.myblog</groupId>
    <artifactId>CommonLib</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

2. エンティティで `BasicEntity` を継承するだけでよい(追加設定は不要)

```java
@Entity
@Table(name = "article")
public class Article extends BasicEntity {
    private String title;
    // getter/setter
}
```

3. `save`/`persist` を呼ぶと、JPAプロバイダが `@PrePersist`/`@PreUpdate` を呼び出して監査項目を補完する
   (`BasicEntity` はMyBatis-Plusのアノテーションに依存しないため、JPAの設定だけで動作する)

補完に使用するユーザーIDは `AuditUserIdHolder` を介して `CurrentUserIdProvider` から取得する。
Spring Boot起動時に `AuditAutoConfiguration` が供給元を設定するため、サービス側の定義は不要
(`common.audit.*` の設定はMyBatisを使うサービスと共通)。

### 仕組み

| クラス | 役割 |
| --- | --- |
| `net.myBlog.common.BasicEntity` | 監査列の定義、`@PrePersist`/`@PreUpdate`(JPA)による補完宣言 |
| `net.myBlog.common.audit.AuditInterceptor` | MyBatis用。`Executor#update` を横取りして補完するインターセプタ(MyBatis-Plus不要) |
| `net.myBlog.common.audit.AuditUserIdHolder` | JPAコールバック用の供給元(エンティティはSpring管理外のため静的ホルダで共有) |
| `net.myBlog.common.audit.CurrentUserIdProvider` | 操作ユーザーIDの取得インタフェース |
| `net.myBlog.common.audit.SecurityContextCurrentUserIdProvider` | SecurityContext(JWTのsubject)からIDを取得する既定実装 |
| `net.myBlog.common.audit.AuditProperties` | `common.audit.*` の設定バインド |
| `net.myBlog.common.configs.AuditAutoConfiguration` | ユーザーID供給元を自動構成(全サービス共通) |
| `net.myBlog.common.configs.MybatisAuditAutoConfiguration` | MyBatis用インターセプタを自動構成(MyBatis依存時のみ) |

- MyBatisを使わないサービス(JPAなど)では `MybatisAuditAutoConfiguration` は構成されない(`@ConditionalOnClass`)。
- MyBatisを使うサービスでは、`Interceptor` のBeanがMyBatis(およびMyBatis-Plus)の自動構成に取り込まれる。
- 補完はSQL生成前(Executor段階)に行われるため、SQLのカラム有無(MyBatis-Plusのnull項目省略など)にも影響しない。
- JPAを使うサービスではMyBatisもMyBatis-Plusも不要(`@PrePersist`/`@PreUpdate`)。
- JPAのコールバックはSpring管理外のエンティティから呼ばれるため、供給元は `AuditUserIdHolder` を介して共有する
  (Spring Data JPAの `AuditingEntityListener` と同じ方式)。
- MyBatis-Plusを使う場合、主キーは `mybatis-plus.global-config.db-config.id-type: auto` で
  DBのAUTO_INCREMENTに採番させる(`BasicEntity` に `@TableId` を付けないため)。
- 明示的に値を設定して登録する場合(過去データの移行など)は、INSERT時はその値が優先される。

package net.myBlog.common;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import net.myBlog.common.audit.AuditUserIdHolder;

import java.time.OffsetDateTime;

/**
 * 全テーブル共通の監査項目(登録者・登録日時・更新者・更新日時)を持つ基底エンティティ。
 *
 * <p>各サービスのエンティティが本クラスを継承することで、監査項目の列定義と
 * 自動補完の設定を引き継ぐ。補完の実現方法はデータアクセス方式によって異なるが、
 * 補完される内容(登録者/登録日時/更新者/更新日時)と上書き規則は同じである。</p>
 *
 * <ul>
 *     <li>MyBatis / MyBatis-Plus: {@code net.myBlog.common.audit.AuditInterceptor}
 *         (MyBatisのインターセプタ。アノテーション不要でXMLマッパーでも動作する)</li>
 *     <li>JPA/Hibernate: {@link PrePersist}/{@link PreUpdate}(MyBatis不要)</li>
 * </ul>
 *
 * <p>本クラスは特定のフレームワークのアノテーションに依存しない。
 * 列名はJPAでは{@link Column}で明示し、MyBatis-Plusでは
 * camelCase→snake_caseの命名規則により対応付けられる
 * ({@code createUserId}→{@code create_user_id})。</p>
 *
 * <p>使用例(データアクセスの実装を問わず、継承するだけでよい):</p>
 * <pre>{@code
 * @Entity                 // JPAを使う場合
 * @TableName("article")    // MyBatis-Plusを使う場合は省略可(クラス名からarticleを導出)
 * public class Article extends BasicEntity {
 *     private String title;
 *     // getter/setter
 * }
 * }</pre>
 *
 * <p>MyBatis-Plusを使う場合、主キーはDBのAUTO_INCREMENTに採番させるため
 * {@code mybatis-plus.global-config.db-config.id-type: auto} を設定すること
 * (未設定の場合、MyBatis-Plusは既定でスノーフローIDを採番してしまう)。</p>
 */
@MappedSuperclass
public class BasicEntity {

    /** 主キー(DBのAUTO_INCREMENTで採番) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 登録者ユーザーID(INSERT時に自動補完) */
    @Column(name = "create_user_id")
    private Integer createUserId;

    /** 登録日時(INSERT時に自動補完) */
    @Column(name = "create_time")
    private OffsetDateTime createTime;

    /** 更新者ユーザーID(INSERT/UPDATE時に自動補完) */
    @Column(name = "update_user_id")
    private Integer updateUserId;

    /** 更新日時(INSERT/UPDATE時に自動補完) */
    @Column(name = "update_time")
    private OffsetDateTime updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Integer createUserId) {
        this.createUserId = createUserId;
    }

    public OffsetDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(OffsetDateTime createTime) {
        this.createTime = createTime;
    }

    public Integer getUpdateUserId() {
        return updateUserId;
    }

    public void setUpdateUserId(Integer updateUserId) {
        this.updateUserId = updateUserId;
    }

    public OffsetDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(OffsetDateTime updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * INSERT前に監査項目を補完する(JPA/Hibernateが呼び出す)。
     *
     * <p>MyBatis-Plus経由の登録ではJPAプロバイダが存在しないため呼び出されず、
     * 代わりに{@code AuditMetaObjectHandler}が補完する。
     * 呼び出し側が明示的に設定した値(過去データの移行など)は尊重する。</p>
     */
    @PrePersist
    void fillAuditOnPersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createUserId == null) {
            createUserId = AuditUserIdHolder.currentUserId();
        }
        if (createTime == null) {
            createTime = now;
        }
        if (updateUserId == null) {
            updateUserId = createUserId;
        }
        if (updateTime == null) {
            updateTime = now;
        }
    }

    /**
     * UPDATE前に更新者・更新日時を最新へ上書きする(JPA/Hibernateが呼び出す)。
     *
     * <p>更新の監査項目は「最後に更新した人・日時」である必要があるため、
     * 呼び出し側の値に関わらず常に上書きする。</p>
     */
    @PreUpdate
    void fillAuditOnUpdate() {
        Integer userId = AuditUserIdHolder.currentUserId();
        if (userId != null) {
            updateUserId = userId;
        }
        updateTime = OffsetDateTime.now();
    }
}

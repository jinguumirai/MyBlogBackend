package net.myBlog.common.audit;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 監査項目(登録者・登録日時・更新者・更新日時)を自動補完するMyBatisインターセプタ。
 *
 * <p>MyBatis本体の{@link Executor#update}を横取りして補完するため、
 * <strong>MyBatis-Plusに依存せず</strong>、プレーンMyBatis(XMLマッパー)でも
 * MyBatis-Plusでも同じように動作する。エンティティ側のアノテーションは不要で、
 * プロパティ名({@code createUserId}など)の一致で補完する。</p>
 *
 * <ul>
 *     <li>INSERT: {@code create_user_id} / {@code create_time} / {@code update_user_id} / {@code update_time}
 *         (呼び出し側で明示的に設定済みの値は上書きしない)</li>
 *     <li>UPDATE: {@code update_user_id} / {@code update_time}(常に最新の操作者・日時で上書きする)</li>
 * </ul>
 *
 * <p>補完はSQLの生成前(Executor段階)に行われるため、MyBatis-Plusの
 * {@code MetaObjectHandler}を使う場合と同じ結果になる。バッチ登録(Collection/配列)は
 * 各要素を補完し、単一パラメータをMapで受ける場合({@code @Param("et")})も補完する。</p>
 *
 * <p>MyBatis-Plusを使う場合、主キーは{@code @TableId}を付けずに
 * {@code mybatis-plus.global-config.db-config.id-type: auto} を設定して
 * DBのAUTO_INCREMENTに採番させること(未設定だとMPがスノーフローIDを採番してしまう)。</p>
 *
 * <p>Bean登録は{@code net.myBlog.common.configs.MybatisAuditAutoConfiguration}が行い、
 * Bean定義するだけでMyBatisへ自動的に適用される。</p>
 */
@Intercepts(@Signature(type = Executor.class, method = "update",
        args = {MappedStatement.class, Object.class}))
public class AuditInterceptor implements Interceptor {

    /** {@code BasicEntity}の登録者ユーザーIDプロパティ名 */
    private static final String PROPERTY_CREATE_USER_ID = "createUserId";

    /** {@code BasicEntity}の登録日時プロパティ名 */
    private static final String PROPERTY_CREATE_TIME = "createTime";

    /** {@code BasicEntity}の更新者ユーザーIDプロパティ名 */
    private static final String PROPERTY_UPDATE_USER_ID = "updateUserId";

    /** {@code BasicEntity}の更新日時プロパティ名 */
    private static final String PROPERTY_UPDATE_TIME = "updateTime";

    /** 単一パラメータをMapで受ける場合にエンティティを取り出すキー */
    private static final String ENTITY_KEY = "et";

    private final CurrentUserIdProvider currentUserIdProvider;

    /** 現在日時の取得に使用するクロック(テストで固定するために差し替え可能) */
    private final Clock clock;

    public AuditInterceptor(CurrentUserIdProvider currentUserIdProvider) {
        this(currentUserIdProvider, Clock.systemDefaultZone());
    }

    AuditInterceptor(CurrentUserIdProvider currentUserIdProvider, Clock clock) {
        this.currentUserIdProvider = currentUserIdProvider;
        this.clock = clock;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs().length > 1 ? invocation.getArgs()[1] : null;
        if (isAuditTarget(mappedStatement.getSqlCommandType())) {
            for (Object entity : resolveEntities(parameter)) {
                fillAuditFields(mappedStatement.getSqlCommandType(), entity);
            }
        }
        return invocation.proceed();
    }

    /** 監査項目を補完する(エンティティが対象プロパティを持たない場合は何もしない)。 */
    private void fillAuditFields(SqlCommandType sqlCommandType, Object entity) {
        MetaObject metaObject = SystemMetaObject.forObject(entity);
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (sqlCommandType == SqlCommandType.INSERT) {
            Integer userId = currentUserIdProvider.getCurrentUserId();
            fillIfAbsent(metaObject, PROPERTY_CREATE_USER_ID, userId);
            fillIfAbsent(metaObject, PROPERTY_CREATE_TIME, now);
            fillIfAbsent(metaObject, PROPERTY_UPDATE_USER_ID, userId);
            fillIfAbsent(metaObject, PROPERTY_UPDATE_TIME, now);
        } else {
            fill(metaObject, PROPERTY_UPDATE_USER_ID, currentUserIdProvider.getCurrentUserId());
            fill(metaObject, PROPERTY_UPDATE_TIME, now);
        }
    }

    /** 補完対象のSQLか(INSERT/UPDATEのみ) */
    private boolean isAuditTarget(SqlCommandType sqlCommandType) {
        return sqlCommandType == SqlCommandType.INSERT || sqlCommandType == SqlCommandType.UPDATE;
    }

    /**
     * 補完対象のエンティティを解決する。
     *
     * <p>バッチ登録(Collection/配列)の場合は各要素を、
     * 単一パラメータをMapで受ける場合({@code @Param("et")})はその値を返す。</p>
     */
    private List<Object> resolveEntities(Object parameter) {
        if (parameter == null) {
            return List.of();
        }
        if (parameter instanceof Map<?, ?> map) {
            Object entity = map.get(ENTITY_KEY);
            return entity == null ? List.of() : List.of(entity);
        }
        if (parameter instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        if (parameter.getClass().isArray()) {
            if (parameter.getClass().getComponentType().isPrimitive()) {
                return List.of();
            }
            return Arrays.asList((Object[]) parameter);
        }
        return List.of(parameter);
    }

    /** 該当プロパティを持ち、未設定(null)の場合のみ補完する。 */
    private void fillIfAbsent(MetaObject metaObject, String property, Object value) {
        if (metaObject.hasSetter(property) && metaObject.getValue(property) == null) {
            fill(metaObject, property, value);
        }
    }

    private void fill(MetaObject metaObject, String property, Object value) {
        if (value != null && metaObject.hasSetter(property)) {
            metaObject.setValue(property, value);
        }
    }
}

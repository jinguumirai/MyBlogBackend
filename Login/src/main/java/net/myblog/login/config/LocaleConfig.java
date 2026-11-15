package net.myblog.login.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * フロントエンドの言語コード（jp / en / cn）を Java ロケールに正規化する。
 * フロントエンドが Accept-Language ヘッダーで「jp」「cn」を送ってきた場合でも、
 * それぞれ日本語（ja_JP）、中国語（zh_CN）のメッセージが解決されるようにする。
 */
@Configuration
public class LocaleConfig {

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver() {
            @Override
            public Locale resolveLocale(HttpServletRequest request) {
                String header = request.getHeader("Accept-Language");
                if (header != null && !header.isBlank()) {
                    String lang = header.split(",")[0].trim().toLowerCase();
                    if (lang.startsWith("jp")) {
                        return Locale.JAPAN; // jp -> ja_JP
                    }
                    if (lang.startsWith("cn")) {
                        return Locale.SIMPLIFIED_CHINESE; // cn -> zh_CN
                    }
                }
                return super.resolveLocale(request);
            }
        };
        resolver.setSupportedLocales(List.of(
                Locale.JAPAN, Locale.JAPANESE,     // ja_JP / ja
                Locale.SIMPLIFIED_CHINESE, Locale.CHINESE, // zh_CN / zh
                Locale.ENGLISH, Locale.US, Locale.UK));    // en / en_US / en_UK
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }
}

package net.myBlog.common.handlers;

import net.myBlog.common.properties.ExceptionProperties;
import net.myBlog.common.exceptions.SystemException;
import net.myBlog.common.responses.ApiResponse;
import net.myBlog.common.utils.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.logging.Logger;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ExceptionProperties exceptionProperties;
    private final MessageSource messageSource;
    private final Logger logger;

    public GlobalExceptionHandler(ExceptionProperties exceptionProperties, MessageSource messageSource) {
        this.exceptionProperties = exceptionProperties;
        this.messageSource = messageSource;
        logger = Logger.getLogger(GlobalExceptionHandler.class.getName());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception e) {
        logger.warning(Arrays.toString(e.getStackTrace()));
        return ResponseEntity.status(500).body(ApiResponse.error("500", e.getMessage(), Boolean.TRUE.equals(exceptionProperties.getShowDetail()) ? Arrays.toString(e.getStackTrace()) : null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        logger.warning(Arrays.toString(e.getStackTrace()));
        return ResponseEntity.status(400).body(ApiResponse.error("400", e.getMessage(), Boolean.TRUE.equals(exceptionProperties.getShowDetail()) ? Arrays.toString(e.getStackTrace()) : null));
    }

    @ExceptionHandler(SystemException.class)
    public ResponseEntity<ApiResponse<String>> handleSystemException(SystemException e) {
        logger.warning(Arrays.toString(e.getStackTrace()));
        String detail = Boolean.TRUE.equals(exceptionProperties.getShowDetail()) ? Arrays.toString(e.getStackTrace()) : null;
        // エラーコードを i18n メッセージキーとして、リクエストのロケールに応じたメッセージを解決する。
        // コードに対応するメッセージが無い場合は SystemException の message をフォールバックとして返す。
        String message = resolveI18nMessage(e.getCode(), e.getMessage(), e.getParameters());
        return ResponseEntity.status(200).body(ApiResponse.error(e.getCode(), message, detail));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleNoResourceFoundException(NoResourceFoundException e) {
        logger.warning(Arrays.toString(e.getStackTrace()));
        return ResponseEntity.status(404).body(ApiResponse.<String>error("404", e.getMessage(), null));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        logger.warning(Arrays.toString(e.getStackTrace()));
        return ResponseEntity.status(404).body(ApiResponse.<String>error("404", e.getMessage(), null));
    }

    // 認証失敗 → 401 Unauthorized
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<String>> handleAuthenticationException(AuthenticationException e) {
        logger.warning(e.getMessage());
        return buildErrorResponse(401, "Authentication failed", null);
    }

    // 認可失敗 → 403 Forbidden
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<String>> handleAccessDeniedException(AccessDeniedException e) {
        logger.warning(e.getMessage());
        return buildErrorResponse(403, "Access denied", null);
    }

    // リクエストボディ読み取り失敗 → 400 Bad Request
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        logger.warning(e.getMessage());
        return buildErrorResponse(400, e.getMessage(), null);
    }

    // 必須パラメータ欠落 → 400 Bad Request
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<String>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        logger.warning(e.getMessage());
        return buildErrorResponse(400, "Required parameter '" + e.getParameterName() + "' is missing", null);
    }

    // パラメータ型不一致 → 400 Bad Request
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<String>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        logger.warning(e.getMessage());
        return buildErrorResponse(400, "Parameter '" + e.getName() + "' has invalid type", null);
    }

    // メソッド不一致 → 405 Method Not Allowed
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<String>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        logger.warning(e.getMessage());
        return buildErrorResponse(405, e.getMessage(), null);
    }

    // Content-Type 不一致 → 415 Unsupported Media Type
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<String>> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        logger.warning(e.getMessage());
        return buildErrorResponse(415, e.getMessage(), null);
    }

    // レスポンス形式不一致 → 406 Not Acceptable
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiResponse<String>> handleHttpMediaTypeNotAcceptableException(HttpMediaTypeNotAcceptableException e) {
        logger.warning(e.getMessage());
        return buildErrorResponse(406, e.getMessage(), null);
    }

    private ResponseEntity<ApiResponse<String>> buildErrorResponse(int status, String message, String detail) {
        return ResponseEntity.status(status).body(ApiResponse.error(String.valueOf(status), message, detail));
    }

    /**
     * SystemException のエラーコードを i18n メッセージキーとして、
     * リクエストのロケール（Accept-Language など）に応じたメッセージを解決する。
     *
     * @param code           i18n メッセージキー（SystemException のエラーコード）
     * @param defaultMessage キー未登録時にフォールバックするメッセージ
     */
    private String resolveI18nMessage(String code, String defaultMessage, Object[] args) {
        if (StringUtils.isBlank(code) || messageSource == null) {
            return defaultMessage;
        }
        return messageSource.getMessage(code, args, defaultMessage, LocaleContextHolder.getLocale());
    }
}

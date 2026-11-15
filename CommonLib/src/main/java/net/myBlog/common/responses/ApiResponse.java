package net.myBlog.common.responses;

import net.myBlog.common.exceptions.SystemException;
import net.myBlog.common.utils.StringUtils;

import java.util.Arrays;

public class ApiResponse<T> {
    String code;
    String message;
    T data;

    public ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public static<T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("0", null, data);
    }

    public static<T> ApiResponse<T> success() {
        return new ApiResponse<>("0", null, null);
    }

    public static<T> ApiResponse<T> error(String code, String message, T data) {
        if (StringUtils.isBlank(code) || code.equals("0")) {
            throw new RuntimeException("Error code cannot be null or empty or zero");
        }
        return new ApiResponse<>(code, message, data);
    }

    public static ApiResponse<String> error(SystemException systemException) {
        return ApiResponse.<String>error(systemException.getCode(), systemException.getMessage(), Arrays.toString(systemException.getStackTrace()));
    }
}

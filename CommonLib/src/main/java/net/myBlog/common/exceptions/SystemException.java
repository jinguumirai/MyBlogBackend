package net.myBlog.common.exceptions;

public class SystemException extends RuntimeException {
    private String code;
    private String message;
    private Object[] parameters;

    public SystemException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public SystemException(String code, String message, Object[] parameters)
    {
        super(message);
        this.code = code;
        this.message = message;
        this.parameters = parameters;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

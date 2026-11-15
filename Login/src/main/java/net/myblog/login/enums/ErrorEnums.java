package net.myblog.login.enums;

public enum ErrorEnums {
    NOSUCHUSERORPASSWORDERROR("NOSUCHUSERORPASSWORDERROR"),
    /** すでに登録済み(メール認証済み)のメールアドレスで登録しようとした場合 */
    EMAILALREADYREGISTERED("EMAILALREADYREGISTERED"),
    /** 認証コードが未送信・不一致・使用済みの場合 */
    INVALIDVERIFICATIONCODE("INVALIDVERIFICATIONCODE");

    private final String code;
    ErrorEnums(String code) {
        this.code = code;
    }
    public String getCode()
    {
        return code;
    }
}

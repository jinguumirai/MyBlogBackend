package net.myBlog.common.properties;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "common.exception")
public class ExceptionProperties {

    private Boolean enable = true;
    private Boolean showDetail = false;

    public ExceptionProperties(Boolean enable, Boolean showDetail) {
        this.enable = enable;
        this.showDetail = showDetail;
    }

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public Boolean getShowDetail() {
        return showDetail;
    }

    public void setShowDetail(Boolean showDetail) {
        this.showDetail = showDetail;
    }
}

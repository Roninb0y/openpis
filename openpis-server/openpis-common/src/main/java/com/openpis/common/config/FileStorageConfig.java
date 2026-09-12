package com.openpis.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "file")
public class FileStorageConfig {

    private Upload upload = new Upload();
    private Access access = new Access();

    @Getter
    @Setter
    public static class Upload {
        private String path = "./uploads/";
    }

    @Getter
    @Setter
    public static class Access {
        private String url = "http://localhost:8080/uploads";
    }

    /**
     * 拼接文件的完整访问 URL：access.url + "/" + 相对路径
     */
    public String fileUrl(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return "";
        }
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath;
        }
        String base = access.getUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String path = relativePath.replace("\\", "/");
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return base + "/" + path;
    }
}

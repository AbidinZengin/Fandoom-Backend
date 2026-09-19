package com.example.fandoom_backend.media.service;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class CloudinaryMediaUrlValidator implements MediaUrlValidator {

    private static final String HOST = "res.cloudinary.com";

    private final Cloudinary cloudinary;

    @Override
    public boolean isOwnedImage(String url) {
        return uploadPath(url, "image") != null;
    }

    @Override
    public boolean isOwnedVideo(String url) {
        return uploadPath(url, "video") != null;
    }

    @Override
    public boolean isInFolder(String url, String folder) {
        String path = uploadPath(url, "image");
        if (path == null) {
            path = uploadPath(url, "video");
        }
        return path != null
                && Pattern.compile("^(?:v\\d+/)?fandoom/" + Pattern.quote(folder) + "/.+").matcher(path).matches();
    }

    // https://res.cloudinary.com/<cloud>/<resourceType>/upload/<rest> ise <rest>, aksi halde null.
    // Query/fragment/userinfo/port reddedilir; cloud adı CLOUDINARY_URL'den gelir, yoksa hiçbir URL geçerli sayılmaz.
    private String uploadPath(String url, String resourceType) {
        String cloudName = cloudinary.config.cloudName;
        if (!StringUtils.hasText(url) || !StringUtils.hasText(cloudName)) {
            return null;
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return null;
        }
        if (!"https".equals(uri.getScheme()) || !HOST.equalsIgnoreCase(uri.getHost()) || uri.getPort() != -1
                || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            return null;
        }
        String prefix = "/" + cloudName + "/" + resourceType + "/upload/";
        String path = uri.getRawPath();
        if (path == null || !path.startsWith(prefix) || path.length() == prefix.length()
                || path.contains("..") || path.contains("%")) {
            return null;
        }
        return path.substring(prefix.length());
    }
}

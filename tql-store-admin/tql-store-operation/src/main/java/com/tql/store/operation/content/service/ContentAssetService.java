package com.tql.store.operation.content.service;

import com.tql.store.operation.content.model.ContentAssetView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class ContentAssetService {

    private static final long MAX_VIDEO_SIZE = 200L * 1024L * 1024L;
    private static final long MAX_IMAGE_SIZE = 10L * 1024L * 1024L;
    private static final long MAX_AUDIO_SIZE = 50L * 1024L * 1024L;
    private final Path storageRoot;

    public ContentAssetService(
            @Value("${content.asset.storage-root:./data/content-assets}") String storageRoot) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    public ContentAssetView uploadSampleVideo(Long tenantId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择样例视频");
        }
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new IllegalArgumentException("样例视频不能超过200MB");
        }
        String originalName = file.getOriginalFilename() == null
                ? "sample.mp4" : file.getOriginalFilename().trim();
        String lowerName = originalName.toLowerCase(Locale.ROOT);
        String contentType = file.getContentType();
        if (!lowerName.endsWith(".mp4")
                || (contentType != null
                && !contentType.equalsIgnoreCase("video/mp4")
                && !contentType.equalsIgnoreCase("application/octet-stream"))) {
            throw new IllegalArgumentException("样例视频仅支持MP4格式");
        }

        String storedName = UUID.randomUUID() + ".mp4";
        Path tenantDirectory = storageRoot.resolve(String.valueOf(tenantId)).normalize();
        Path target = tenantDirectory.resolve(storedName).normalize();
        ensureInsideStorage(target, tenantDirectory);
        try {
            Files.createDirectories(tenantDirectory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("样例视频保存失败", ex);
        }
        return new ContentAssetView(
                "/api/operation/content-assets/" + tenantId + "/" + storedName,
                originalName,
                file.getSize());
    }

    public Resource loadSampleVideo(Long tenantId, String fileName) {
        if (fileName == null || !fileName.matches("[0-9a-fA-F-]{36}\\.mp4")) {
            throw new IllegalArgumentException("样例视频不存在");
        }
        Path tenantDirectory = storageRoot.resolve(String.valueOf(tenantId)).normalize();
        Path target = tenantDirectory.resolve(fileName).normalize();
        ensureInsideStorage(target, tenantDirectory);
        if (!Files.isRegularFile(target)) {
            throw new IllegalArgumentException("样例视频不存在");
        }
        try {
            return new UrlResource(target.toUri());
        } catch (IOException ex) {
            throw new IllegalStateException("样例视频读取失败", ex);
        }
    }

    public ContentAssetView uploadSampleCover(Long tenantId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择视频封面示例");
        if (file.getSize() > MAX_IMAGE_SIZE) throw new IllegalArgumentException("视频封面示例不能超过10MB");
        String originalName = file.getOriginalFilename() == null ? "cover.jpg" : file.getOriginalFilename().trim();
        String lowerName = originalName.toLowerCase(Locale.ROOT);
        String extension = lowerName.endsWith(".jpeg") ? ".jpeg"
                : lowerName.endsWith(".jpg") ? ".jpg"
                : lowerName.endsWith(".png") ? ".png"
                : lowerName.endsWith(".webp") ? ".webp" : "";
        if (extension.isEmpty()) throw new IllegalArgumentException("视频封面示例仅支持JPG、PNG、WEBP格式");
        String storedName = UUID.randomUUID() + extension;
        Path tenantDirectory = storageRoot.resolve(String.valueOf(tenantId)).resolve("covers").normalize();
        Path target = tenantDirectory.resolve(storedName).normalize();
        ensureInsideStorage(target, tenantDirectory);
        try {
            Files.createDirectories(tenantDirectory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("视频封面示例保存失败", ex);
        }
        return new ContentAssetView("/api/operation/content-assets/covers/" + tenantId + "/" + storedName, originalName, file.getSize());
    }

    public Resource loadSampleCover(Long tenantId, String fileName) {
        if (fileName == null || !fileName.matches("[0-9a-fA-F-]{36}\\.(jpg|jpeg|png|webp)")) {
            throw new IllegalArgumentException("视频封面示例不存在");
        }
        Path tenantDirectory = storageRoot.resolve(String.valueOf(tenantId)).resolve("covers").normalize();
        Path target = tenantDirectory.resolve(fileName).normalize();
        ensureInsideStorage(target, tenantDirectory);
        if (!Files.isRegularFile(target)) throw new IllegalArgumentException("视频封面示例不存在");
        try { return new UrlResource(target.toUri()); }
        catch (IOException ex) { throw new IllegalStateException("视频封面示例读取失败", ex); }
    }

    public ContentAssetView uploadBgm(Long tenantId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择BGM文件");
        if (file.getSize() > MAX_AUDIO_SIZE) throw new IllegalArgumentException("BGM文件不能超过50MB");
        String originalName = file.getOriginalFilename() == null ? "bgm.mp3" : file.getOriginalFilename().trim();
        String lowerName = originalName.toLowerCase(Locale.ROOT);
        String extension = lowerName.endsWith(".wav") ? ".wav" : lowerName.endsWith(".m4a") ? ".m4a" : lowerName.endsWith(".mp3") ? ".mp3" : "";
        if (extension.isEmpty()) throw new IllegalArgumentException("BGM仅支持MP3、WAV、M4A格式");
        String storedName = UUID.randomUUID() + extension;
        Path tenantDirectory = storageRoot.resolve(String.valueOf(tenantId)).resolve("bgm").normalize();
        Path target = tenantDirectory.resolve(storedName).normalize();
        ensureInsideStorage(target, tenantDirectory);
        try {
            Files.createDirectories(tenantDirectory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("BGM文件保存失败", ex);
        }
        return new ContentAssetView("/api/operation/content-assets/bgm/" + tenantId + "/" + storedName, originalName, file.getSize());
    }

    public Resource loadBgm(Long tenantId, String fileName) {
        if (fileName == null || !fileName.matches("[0-9a-fA-F-]{36}\\.(mp3|wav|m4a)")) throw new IllegalArgumentException("BGM文件不存在");
        Path tenantDirectory = storageRoot.resolve(String.valueOf(tenantId)).resolve("bgm").normalize();
        Path target = tenantDirectory.resolve(fileName).normalize();
        ensureInsideStorage(target, tenantDirectory);
        if (!Files.isRegularFile(target)) throw new IllegalArgumentException("BGM文件不存在");
        try { return new UrlResource(target.toUri()); }
        catch (IOException ex) { throw new IllegalStateException("BGM文件读取失败", ex); }
    }

    public ContentAssetView uploadMaterial(Long tenantId, String materialType, MultipartFile file) {
        String type = materialType == null ? "" : materialType.trim().toUpperCase(Locale.ROOT);
        if (!type.equals("VIDEO") && !type.equals("IMAGE")) {
            throw new IllegalArgumentException("仅支持上传视频或图片素材");
        }
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择素材文件");
        long maxSize = type.equals("VIDEO") ? MAX_VIDEO_SIZE : MAX_IMAGE_SIZE;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(type.equals("VIDEO") ? "视频素材不能超过200MB" : "图片素材不能超过10MB");
        }
        String originalName = file.getOriginalFilename() == null ? "material" : file.getOriginalFilename().trim();
        String lowerName = originalName.toLowerCase(Locale.ROOT);
        String extension;
        if (type.equals("VIDEO")) {
            extension = lowerName.endsWith(".mp4") ? ".mp4" : "";
            if (extension.isEmpty()) throw new IllegalArgumentException("视频素材仅支持MP4格式");
        } else {
            extension = lowerName.endsWith(".jpeg") ? ".jpeg"
                    : lowerName.endsWith(".jpg") ? ".jpg"
                    : lowerName.endsWith(".png") ? ".png"
                    : lowerName.endsWith(".webp") ? ".webp" : "";
            if (extension.isEmpty()) throw new IllegalArgumentException("图片素材仅支持JPG、PNG、WEBP格式");
        }
        String storedName = UUID.randomUUID() + extension;
        String directoryName = type.toLowerCase(Locale.ROOT);
        Path tenantDirectory = storageRoot.resolve(String.valueOf(tenantId)).resolve("materials").resolve(directoryName).normalize();
        Path target = tenantDirectory.resolve(storedName).normalize();
        ensureInsideStorage(target, tenantDirectory);
        try {
            Files.createDirectories(tenantDirectory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("素材文件保存失败", ex);
        }
        return new ContentAssetView(
                "/api/operation/content-assets/materials/" + tenantId + "/" + directoryName + "/" + storedName,
                originalName,
                file.getSize());
    }

    public Resource loadMaterial(Long tenantId, String materialType, String fileName) {
        String type = materialType == null ? "" : materialType.trim().toLowerCase(Locale.ROOT);
        if (!type.equals("video") && !type.equals("image")) throw new IllegalArgumentException("素材类型不正确");
        String pattern = type.equals("video")
                ? "[0-9a-fA-F-]{36}\\.mp4"
                : "[0-9a-fA-F-]{36}\\.(jpg|jpeg|png|webp)";
        if (fileName == null || !fileName.matches(pattern)) throw new IllegalArgumentException("素材文件不存在");
        Path tenantDirectory = storageRoot.resolve(String.valueOf(tenantId)).resolve("materials").resolve(type).normalize();
        Path target = tenantDirectory.resolve(fileName).normalize();
        ensureInsideStorage(target, tenantDirectory);
        if (!Files.isRegularFile(target)) throw new IllegalArgumentException("素材文件不存在");
        try { return new UrlResource(target.toUri()); }
        catch (IOException ex) { throw new IllegalStateException("素材文件读取失败", ex); }
    }

    public String materialMediaType(String materialType, String fileName) {
        if ("video".equalsIgnoreCase(materialType)) return "video/mp4";
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private void ensureInsideStorage(Path target, Path tenantDirectory) {
        if (!target.startsWith(storageRoot) || !target.startsWith(tenantDirectory)) {
            throw new IllegalArgumentException("非法文件路径");
        }
    }
}

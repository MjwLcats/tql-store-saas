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

    private void ensureInsideStorage(Path target, Path tenantDirectory) {
        if (!target.startsWith(storageRoot) || !target.startsWith(tenantDirectory)) {
            throw new IllegalArgumentException("非法文件路径");
        }
    }
}

package com.openpis.material.service;

import com.openpis.common.config.FileStorageConfig;
import com.openpis.common.exception.BizException;
import com.openpis.material.dto.MaterialUpdateRequest;
import com.openpis.material.entity.Material;
import com.openpis.material.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final FileStorageConfig fileStorageConfig;

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;   // 10MB
    private static final long MAX_VIDEO_SIZE = 200 * 1024 * 1024;  // 200MB
    private static final long MAX_TEXT_SIZE = 1 * 1024 * 1024;     // 1MB

    @Transactional
    public Material upload(MultipartFile file, String description, Integer duration) {
        if (file == null || file.isEmpty()) {
            throw new BizException("文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new BizException("文件名不能为空");
        }

        String type = determineType(originalFilename);
        validateSize(file, type);

        String extension = getExtension(originalFilename);
        String newFileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String relativeDir = "materials/" + type + "/";

        try {
            Path dirPath = Paths.get(fileStorageConfig.getUpload().getPath(), relativeDir).toAbsolutePath();
            Files.createDirectories(dirPath);

            Path filePath = dirPath.resolve(newFileName);
            file.transferTo(filePath.toFile());

            Material material = new Material();
            material.setTitle(originalFilename);
            material.setType(type);
            material.setFilePath(relativeDir + newFileName);
            material.setDescription(description);
            material.setStatus(1);

            if ("image".equals(type)) {
                String thumbnailName = "thumb_" + newFileName;
                Path thumbnailPath = dirPath.resolve(thumbnailName);
                Thumbnails.of(filePath.toFile())
                        .size(300, 300)
                        .keepAspectRatio(true)
                        .toFile(thumbnailPath.toFile());
                material.setThumbnail(relativeDir + thumbnailName);
                material.setDuration(0);
            } else if ("video".equals(type)) {
                // MVP 不做 FFmpeg 解析，视频时长由上传者手填
                material.setDuration(duration != null && duration > 0 ? duration : 0);
            } else {
                material.setDuration(0);
            }

            return materialRepository.save(material);
        } catch (IOException e) {
            throw new BizException("文件保存失败: " + e.getMessage());
        }
    }

    public Page<Material> list(int page, int size, String type, String keyword) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (StringUtils.hasText(type) && StringUtils.hasText(keyword)) {
            return materialRepository.findByTypeAndTitleContainingIgnoreCase(type, keyword, pageable);
        } else if (StringUtils.hasText(type)) {
            return materialRepository.findByType(type, pageable);
        } else if (StringUtils.hasText(keyword)) {
            return materialRepository.findByTitleContainingIgnoreCase(keyword, pageable);
        }
        return materialRepository.findAll(pageable);
    }

    public Optional<Material> findById(Long id) {
        return materialRepository.findById(id);
    }

    @Transactional
    public Material update(Long id, MaterialUpdateRequest request) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new BizException("媒体不存在"));

        if (StringUtils.hasText(request.getTitle())) {
            material.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            material.setDescription(request.getDescription());
        }
        if (request.getDuration() != null && request.getDuration() >= 0) {
            material.setDuration(request.getDuration());
        }
        return materialRepository.save(material);
    }

    @Transactional
    public void delete(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new BizException("媒体不存在"));

        try {
            Path filePath = Paths.get(fileStorageConfig.getUpload().getPath(), material.getFilePath()).toAbsolutePath();
            Files.deleteIfExists(filePath);
            if (StringUtils.hasText(material.getThumbnail())) {
                Path thumbnailPath = Paths.get(fileStorageConfig.getUpload().getPath(), material.getThumbnail()).toAbsolutePath();
                Files.deleteIfExists(thumbnailPath);
            }
        } catch (IOException e) {
            throw new BizException("删除文件失败: " + e.getMessage());
        }

        materialRepository.delete(material);
    }

    private String determineType(String filename) {
        String ext = getExtension(filename).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "image";
            case "mp4", "avi", "mov", "mkv", "flv" -> "video";
            case "txt" -> "text";
            default -> throw new BizException("不支持的文件类型: " + ext);
        };
    }

    private void validateSize(MultipartFile file, String type) {
        long size = file.getSize();
        switch (type) {
            case "image" -> {
                if (size > MAX_IMAGE_SIZE) throw new BizException("图片大小不能超过 10MB");
            }
            case "video" -> {
                if (size > MAX_VIDEO_SIZE) throw new BizException("视频大小不能超过 200MB");
            }
            case "text" -> {
                if (size > MAX_TEXT_SIZE) throw new BizException("文本大小不能超过 1MB");
            }
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex == -1 ? "" : filename.substring(dotIndex + 1);
    }
}

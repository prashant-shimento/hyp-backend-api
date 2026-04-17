package com.hyp.service;

import com.hyp.entity.Partner;
import com.hyp.enums.PartnerType;
import com.hyp.repository.PartnerRepository;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class PartnerService extends BaseServiceImpl<Partner, String> {

    @Autowired
    PartnerRepository partnerRepository;

    @Autowired
    BucketService bucketService;

    private final ExecutorService uploadExecutor = Executors.newFixedThreadPool(8);

    @Override
    protected String cacheName() {
        return "partners";
    }

    @Override
    protected Class<Partner> entityType() {
        return Partner.class;
    }

    public List<Partner> findByPartnerType(PartnerType type) {
        return partnerRepository.findByType(type);
    }

    public Partner findPartnersByRestaurantId(String restaurantId, PartnerType type) {
        if (restaurantId == null) return null;
        String cacheKey = "restaurant:" + restaurantId + ":" + type.name();
        return cacheService()
                .getOrLoad(
                        "partners",
                        cacheKey,
                        Partner.class,
                        () -> partnerRepository.findByRestaurantsContainingAndType(restaurantId, type));
    }

    public void uploadAndSavePartnerImages(String partnerId, MultiValueMap<String, MultipartFile> filesByField) {

        Partner partner = partnerRepository
                .findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("Partner not found: " + partnerId));

        String partnerFolderName = partner.getName().trim().toLowerCase().replaceAll("\\s+", "-");
        String basePath = "onboarding-assets/partner/" + partnerFolderName + "/";

        ConcurrentMap<String, List<String>> uploadedImageUrls = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> asyncUploadTasks = new ArrayList<>();

        filesByField.forEach((fieldName, files) -> {
            if (files == null || files.isEmpty()) return;

            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;

                CompletableFuture<Void> uploadTask = CompletableFuture.runAsync(
                        () -> {
                            try (InputStream inputStream = file.getInputStream();
                                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                                inputStream.transferTo(outputStream);

                                String fileName = Path.of(file.getOriginalFilename())
                                        .getFileName()
                                        .toString();
                                String folderPath = basePath + fieldName + "/";

                                try {
                                    boolean deleted = bucketService.deleteFileBeforeUpload(fileName, folderPath);
                                    if (deleted) {
                                        log.info("Deleted existing file before upload: {}/{}", folderPath, fileName);
                                    } else {
                                        log.debug("No existing file found to delete for: {}/{}", folderPath, fileName);
                                    }
                                } catch (Exception ex) {
                                    log.warn(
                                            "Failed to delete existing file before upload for {}/{}: {}",
                                            folderPath,
                                            fileName,
                                            ex.getMessage(),
                                            ex);
                                }

                                String uploadedUrl = bucketService.uploadFileFromStream(
                                        outputStream, fileName, folderPath, file.getContentType());

                                if (uploadedUrl != null) {
                                    uploadedImageUrls
                                            .computeIfAbsent(
                                                    fieldName, k -> Collections.synchronizedList(new ArrayList<>()))
                                            .add(uploadedUrl);
                                    log.info("Uploaded {} -> {}", file.getOriginalFilename(), uploadedUrl);
                                } else {
                                    log.warn("Upload returned null for file: {}", file.getOriginalFilename());
                                }

                            } catch (Exception e) {
                                log.error(
                                        "Upload failed for field='{}' file='{}': {}",
                                        fieldName,
                                        file.getOriginalFilename(),
                                        e.getMessage(),
                                        e);
                            }
                        },
                        uploadExecutor);

                asyncUploadTasks.add(uploadTask);
            }
        });

        CompletableFuture.allOf(asyncUploadTasks.toArray(new CompletableFuture[0]))
                .join();

        uploadedImageUrls.forEach((fieldName, urls) -> {
            if (urls.isEmpty()) return;
            switch (fieldName) {
                case "logo" -> partner.setLogoUrl(urls.get(0));
                case "headerImageUrls" -> partner.setHeaderImageUrls(String.join(",", urls));
                case "galleryImageUrl" -> partner.setGalleryImageUrl(urls);
                default -> log.info("No mapping found for field '{}'", fieldName);
            }
        });

        partner.setUpdatedAt(LocalDateTime.now());
        partnerRepository.save(partner);
    }
}

package com.hyp.service;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.B2StorageClientFactory;
import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.hyp.request.FileUploadRequest;
import com.hyp.util.FileUtils;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BucketService {

    @Value("${bucket.id}")
    private String bucketId;

    @Value("${bucket.name}")
    private String bucketName;

    @Value("${bucket.key}")
    private String bucketKey;

    @Value("${bucket.url}")
    private String bucketUrl;

    public String uploadItemImageFile(FileUploadRequest fileUploadRequest) throws Exception {
        try (B2StorageClient client =
                B2StorageClientFactory.createDefaultFactory().create(bucketId, bucketKey, "BackBlazeUpload")) {

            B2Bucket bucket = client.getBucketOrNullByName(bucketName);
            if (bucket == null) {
                log.warn("Bucket not found {}", bucketName);
                return null;
            }

            String fileUrl = FileUtils.trimFileUrl(fileUploadRequest.getFileUrl());
            log.info("FileUrl after parsing:{} ", fileUrl);
            String contentType = FileUtils.getContentType(fileUrl);
            if (contentType == null) {
                log.warn("Unsupported file extension for file: {}", fileUrl);
            }
            String fileName = FileUtils.formatFileName(fileUploadRequest.getFileName());
            String folderName = fileUploadRequest.getFolderName();
            String folderPath = folderName.endsWith("/") ? folderName : folderName + "/";
            String fullFilePath = folderPath + fileName + FileUtils.getFileExtension(contentType);

            byte[] fileBytes = FileUtils.downloadFileFromUrl(fileUploadRequest.getFileUrl());
            if (fileBytes == null) {
                log.warn("Failed to download the file from URL: {}", fileUrl);
                return null;
            }

            B2UploadFileRequest uploadRequest = B2UploadFileRequest.builder(
                            bucket.getBucketId(), fullFilePath, contentType, B2ByteArrayContentSource.build(fileBytes))
                    .build();

            B2FileVersion fileVersion = client.uploadSmallFile(uploadRequest);

            return bucketUrl + "/" + bucketName + "/" + fileVersion.getFileName();
        } catch (B2Exception e) {
            log.error("Error uploading file to Backblaze: ", e);
        }
        return null;
    }

    public String uploadFileFromStream(
            OutputStream outputStream, String fileName, String folderPath, String contentType) throws Exception {
        try (B2StorageClient client =
                B2StorageClientFactory.createDefaultFactory().create(bucketId, bucketKey, "BackBlazeUpload")) {

            B2Bucket bucket = client.getBucketOrNullByName(bucketName);
            if (bucket == null) {
                log.warn("Bucket not found: {}", bucketName);
                return null;
            }

            byte[] fileBytes = ((ByteArrayOutputStream) outputStream).toByteArray();

            folderPath = folderPath.endsWith("/") ? folderPath : folderPath + "/";
            String fullFilePath = folderPath + fileName;

            B2UploadFileRequest uploadRequest = B2UploadFileRequest.builder(
                            bucket.getBucketId(), fullFilePath, contentType, B2ByteArrayContentSource.build(fileBytes))
                    .build();

            B2FileVersion fileVersion = client.uploadSmallFile(uploadRequest);

            return bucketUrl + "/" + bucketName + "/" + fileVersion.getFileName();
        } catch (B2Exception e) {
            log.error("Error uploading file to Backblaze: ", e);
            throw e;
        }
    }

    public boolean deleteFileBeforeUpload(String fileName, String folderPath) throws Exception {
        try (B2StorageClient client =
                B2StorageClientFactory.createDefaultFactory().create(bucketId, bucketKey, "BackBlazeDelete")) {

            B2Bucket bucket = client.getBucketOrNullByName(bucketName);
            if (bucket == null) {
                log.warn("Bucket not found: {}", bucketName);
                return false;
            }

            folderPath = folderPath.endsWith("/") ? folderPath : folderPath + "/";
            String fullFilePath = folderPath + fileName;

            B2FileVersion latestFileVersion = client.getFileInfoByName(bucket.getBucketName(), fullFilePath);

            if (latestFileVersion == null) {
                log.warn("File not found: {}", fullFilePath);
                return false;
            }

            client.deleteFileVersion(latestFileVersion.getFileName(), latestFileVersion.getFileId());
            log.info("Deleted file from Backblaze: {}", fullFilePath);
            return true;

        } catch (B2Exception e) {
            log.error("Error deleting file from Backblaze: ", e);
            throw e;
        }
    }
}

package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.configurations.MainConfiguration;
import cz.cas.lib.bankid_registrator.dao.mariadb.MediaRepository;
import cz.cas.lib.bankid_registrator.entities.media.MediaSubmissionType;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.media.Media;
import cz.cas.lib.bankid_registrator.util.StringUtils;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService extends ServiceAbstract
{
    private final MainConfiguration mainConfig;
    private final MediaRepository mediaRepository;

    public MediaService(MainConfiguration mainConfig, MediaRepository mediaRepository) {
        super(null);
        this.mainConfig = mainConfig;
        this.mediaRepository = mediaRepository;
    }

    /**
     * Create a shared submission batch ID for all media uploaded from the same form submission.
     * @return batch ID
     */
    public String createSubmissionBatchId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
        return timestamp + "_" + StringUtils.generateRandomAlphanumeric(6).toLowerCase();
    }

    /**
     * Upload a media file
     * @param file
     * @param identity
     * @param submissionType
     * @param submissionBatchId
     * @param batchOrderIndex
     * @return Map<String, Object>
     */
    public Map<String, Object> uploadMedia(MultipartFile file, Identity identity, MediaSubmissionType submissionType, String submissionBatchId, int batchOrderIndex)
    {
        Map<String, Object> result = new HashMap<>();

        String contentType = file.getContentType();
        String originalFileName = file.getOriginalFilename();

        if (contentType == null || !contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("application/pdf")) {
            result.put("error", "Unsupported file type: " + contentType);
            return result;
        }

        String fileName = buildStoredFileName(submissionBatchId, batchOrderIndex, contentType);
        Path path = Paths.get(this.mainConfig.getStorage_path(), fileName);
        try {
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            result.put("error", "Failed to save file: " + e.getMessage());
            return result;
        }

        Media media = new Media();
        media.setName(fileName);
        media.setOriginalName(sanitizeOriginalFileName(originalFileName));
        media.setType(contentType);
        media.setPath(path.toString());
        media.setSubmissionType(submissionType);
        media.setSubmissionBatchId(submissionBatchId);
        media.setBatchOrderIndex(batchOrderIndex);
        media.setIdentity(identity);

        if (this.mediaRepository.save(media) != null) {
            result.put("success", Boolean.TRUE);
        } else {
            result.put("error", "Error uploading media file " + fileName + ".");
        }

        return result;
    }

    /**
     * Find media by identity ID
     * @param identityId
     * @return
     */
    public List<Media> findByIdentityId(Long identityId) {
        List<Media> media = mediaRepository.findByIdentityId(identityId);
        media.sort(
            Comparator.comparing(Media::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(Media::getSubmissionBatchId, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(Media::getBatchOrderIndex, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(Media::getId, Comparator.nullsFirst(Comparator.naturalOrder()))
        );
        return media;
    }

    /**
     * Delete a media file
     * @param media
     * @throws RuntimeException
     */
    public void delete(Media media) {
        Path filePath = Paths.get(media.getPath());

        try {
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e); 
        }
            
        mediaRepository.delete(media);
    }

    private String buildStoredFileName(String submissionBatchId, int batchOrderIndex, String contentType) {
        return submissionBatchId + "_" + String.format("%02d", batchOrderIndex) + getExtensionForContentType(contentType);
    }

    private String getExtensionForContentType(String contentType) {
        switch (contentType) {
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "application/pdf":
                return ".pdf";
            default:
                return "";
        }
    }

    private String sanitizeOriginalFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            return null;
        }

        return Paths.get(originalFileName).getFileName().toString();
    }
}

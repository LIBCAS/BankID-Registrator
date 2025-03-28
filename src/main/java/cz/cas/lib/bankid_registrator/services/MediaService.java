package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.configurations.MainConfiguration;
import cz.cas.lib.bankid_registrator.dao.mariadb.MediaRepository;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.media.Media;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
     * Upload a media file
     * @param file
     * @param identity
     * @return Map<String, Object>
     */
    public Map<String, Object> uploadMedia(MultipartFile file, Identity identity)
    {
        Map<String, Object> result = new HashMap<>();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String contentType = file.getContentType();
        String originalFileName = file.getOriginalFilename();
        String fileName = originalFileName.length() > 10 ? timestamp : (timestamp + "_" + originalFileName);

        if (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("application/pdf")) {
            result.put("error", "Unsupported file type: " + contentType);
            return result;
        }

        Path path = Paths.get(this.mainConfig.getStorage_path(), fileName);
        try {
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            result.put("error", "Failed to save file: " + e.getMessage());
            return result;
        }

        Media media = new Media();
        media.setName(fileName);
        media.setType(contentType);
        media.setPath(path.toString());
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
        return mediaRepository.findByIdentityId(identityId);
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
}

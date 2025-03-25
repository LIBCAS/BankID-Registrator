package cz.cas.lib.bankid_registrator.validators;

import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import cz.cas.lib.bankid_registrator.entities.patron.PatronBoolean;
import cz.cas.lib.bankid_registrator.entities.patron.PatronLanguage;
import cz.cas.lib.bankid_registrator.services.AlephService;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validator for PatronDTO, validates user submitted data from the forms (new membership, membership renewal)
 */
@Component
public class PatronDTOValidator implements Validator
{
    private static final int MAX_FILES = 3; // Maximum number of files that can be uploaded
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // Maximum file size in bytes
    private static final List<String> ALLOWED_FILE_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/png", "application/pdf");

    @Autowired
    private AlephService alephService;

    @Override
    public boolean supports(Class<?> clazz) {
        return PatronDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
    }

    /**
     * Validates the PatronDTO object submitted from the form together with the uploaded media files 
     * @param target
     * @param errors
     * @param patronId
     * @param mediaFiles
     */
    public void validate(Object target, Errors errors, @Nullable String patronId, @Nullable MultipartFile[] mediaFiles) {
        PatronDTO patron = (PatronDTO) target;

        // Validate declaration4
        if (!patron.getIsCasEmployee() && !patron.getDeclaration4()) {
            errors.rejectValue("declaration4", "form.error.field.required", "You must commit to paying the registration fee");
        }

        // Validate RFID
        if (patron.getRfid() != null && !patron.getRfid().isEmpty()) {
            if (alephService.isRfidInUse(patron.getRfid(), patronId)) {
                errors.rejectValue("rfid", "form.error.rfid.notAvailable", "This RFID is not available");
            }
        }

        // Validate Email
        if (patron.getEmail() != null && !patron.getEmail().isEmpty()) {
            if (alephService.isEmailInUse(patron.getEmail(), patronId)) {
                errors.rejectValue("email", "form.error.email.notAvailable", "This email is not available");
            }
        }

        // Validate Export Consent
        if (patron.getExportConsent() != PatronBoolean.Y) {
            errors.rejectValue("exportConsent", "form.error.field.required", "Export consent must be accepted");
        }

        // Validate language - must be one of the supported languages
        if (patron.getConLng() != PatronLanguage.CZE && patron.getConLng() != PatronLanguage.ENG) {
            errors.rejectValue("conLng", "form.error.field.invalid", "Invalid language");
        }

        // CAS Employee validation: either email or media files must be provided
        if (patron.getIsCasEmployee()) {
            int mediaFilesCount = 0;
            if (mediaFiles != null) {
                mediaFilesCount = (int) Arrays.stream(mediaFiles).filter(file -> file != null && !file.isEmpty()).count();
            }

            boolean hasEmail = patron.getEmail() != null && !patron.getEmail().isEmpty();
            boolean hasMediaFiles = mediaFiles != null && mediaFilesCount > 0;

            if (hasMediaFiles) {
                validateMediaFiles(mediaFiles, errors);
            }

            if (!hasEmail && !hasMediaFiles) {
                String defaultErrorMsg = "CAS employees must provide either a valid email address or upload media files.";
                errors.rejectValue("email", "form.error.email.requiredOfCasEmployees", defaultErrorMsg);
                errors.reject("form.error.media.requiredOfCasEmployees", defaultErrorMsg);
            }
        }
    }

    /**
     * Validates the uploaded media files
     * @param mediaFiles
     * @param errors
     */
    private void validateMediaFiles(MultipartFile[] mediaFiles, Errors errors) {
        if (mediaFiles.length > MAX_FILES) {
            errors.reject("form.error.media.filesCountExceeded", new Object[]{MAX_FILES}, "Maximum {0} files are allowed");
        }

        for (MultipartFile file : mediaFiles) {
            if (file.getSize() > MAX_FILE_SIZE) {
                errors.reject("form.error.media.fileSizeExceeded", new Object[]{file.getOriginalFilename(), MAX_FILE_SIZE / (1024 * 1024)}, "File {0} exceeds the maximum size of {1}MB");
            }

            if (!ALLOWED_FILE_CONTENT_TYPES.contains(file.getContentType())) {
                errors.reject("form.error.media.invalidFileType", new Object[]{file.getOriginalFilename()}, "File {0} is not of an allowed type (.jpg, .jpeg, .png, .pdf)");
            }
        }
    }
}
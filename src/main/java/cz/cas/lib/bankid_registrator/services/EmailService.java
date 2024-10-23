package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.configurations.EmailConfig;
import java.util.Locale;
import javax.annotation.Nullable;
import javax.mail.internet.MimeMessage;

import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

@Service
public class EmailService extends ServiceAbstract
{
    private final EmailConfig emailConfig;
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final IdentityActivityService identityActivityService;

    public EmailService(
        MessageSource messageSource,
        EmailConfig emailConfig,
        JavaMailSender mailSender,
        SpringTemplateEngine templateEngine, 
        IdentityActivityService identityActivityService
    ) {
        super(messageSource);
        this.emailConfig = emailConfig;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.identityActivityService = identityActivityService;
    }

    /**
     * Obfuscate the email address by replacing the first half of the email address with asterisks
     * @param email
     */
    public String getObfuscatedEmail(String email)
    {
        String[] parts = email.split("@");
        String obfuscated = parts[0].substring(0, parts[0].length() / 2).replaceAll(".", "*") + "@" + parts[1];
        return obfuscated;
    }

    /**
     * Send an email to the specified address with the specified subject and template.
     * @param to
     * @param subject
     * @param templateName
     * @param context
     * @throws Exception
     */
    public void sendEmail(String to, String subject, String templateName, @Nullable Context context) throws Exception
    {
        String body = templateEngine.process("emails/" + templateName, context);
        String from = String.format("\"%s\" <%s>", emailConfig.getFromName(), emailConfig.getFrom());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true);
        mailSender.send(message);
    }

    /**
     * Send a new registration confirmation email to the newly created Aleph patron
     * @param to
     * @param alephPatronBarcode
     * @param isEmployee - whether the patron is an employee of the CAS
     * @param membershipExpiryDate
     * @param locale
     * @throws MailException
     */
    public void sendEmailNewRegistration(String to, String alephPatronBarcode, boolean isEmployee, String membershipExpiryDate, Locale locale) throws Exception
    {
        Context context = new Context();
        context.setVariable("alephPatronBarcode", alephPatronBarcode);
        context.setVariable("isEmployee", isEmployee);
        context.setVariable("membershipExpiryDate", membershipExpiryDate);

        this.sendEmail(
            to, 
            this.messageSource.getMessage("email.newRegistration.subject", null, locale), 
            locale + "/new_registration_success", 
            context
        );
    }

    /**
     * Send a membership renewal request confirmation email
     * @param to
     * @param alephPatronBarcode
     * @param isEmployee - whether the patron is an employee of the CAS
     * @param membershipExpiryDate
     * @param locale
     * @throws MailException
     */
    public void sendEmailMembershipRenewal(String to, String alephPatronBarcode, boolean isEmployee, String membershipExpiryDate, Locale locale) throws Exception
    {
        Context context = new Context();
        context.setVariable("alephPatronBarcode", alephPatronBarcode);
        context.setVariable("isEmployee", isEmployee);
        context.setVariable("membershipExpiryDate", membershipExpiryDate);

        this.sendEmail(
            to, 
            this.messageSource.getMessage("email.membershipRenewal.subject", null, locale), 
            locale + "/membership_renewal_success", 
            context
        );
    }

    /**
     * Send an email with the link for resetting the identity password
     * @param to
     * @param link
     * @param locale
     * @throws MailException
     */
    public void sendEmailIdentityPasswordReset(String to, String link, Locale locale) throws Exception
    {
        Context context = new Context();
        context.setVariable("pswResetLink", link);

        this.sendEmail(
            to, 
            this.messageSource.getMessage("email.identityPasswordReset.subject", null, locale), 
            locale + "/identity_password_reset", 
            context
        );
    }
}
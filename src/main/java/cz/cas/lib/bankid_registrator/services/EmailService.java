package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.configurations.EmailConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.annotation.Nullable;
import javax.mail.internet.MimeMessage;

@Service
public class EmailService
{
    @Autowired
    private EmailConfig emailConfig;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

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

    public void sendEmailNewRegistration(String to, String registrationId) throws Exception
    {
        Context context = new Context();
        context.setVariable("registrationId", registrationId);

        this.sendEmail(to, "Nová registrace", "new_registration_success", context);
    }
}
package cz.cas.lib.bankid_registrator.configurations;

import javax.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("spring.mail.properties.mail.smtp")
public class EmailConfig
{
    @NotBlank
    private String from;

    @NotBlank
    private String fromName;

    public String getFrom()
    {
        return from;
    }

    public void setFrom(String from)
    {
        this.from = from;
    }

    public String getFromName()
    {
        return fromName;
    }

    public void setFromName(String fromName)
    {
        this.fromName = fromName;
    }
}
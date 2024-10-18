package cz.cas.lib.bankid_registrator.selenium;

import cz.cas.lib.bankid_registrator.controllers.TestMainController;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestMainController.class)
public class CallbackRegistrationNewTest
{
    @LocalServerPort
    private int port;

    private WebDriver driver;

    private static final Logger logger = LoggerFactory.getLogger(CallbackRegistrationNewTest.class);

    @BeforeEach
    public void setUp()
    {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments(
            "--no-sandbox", 
            "--headless", 
            "--disable-gpu", 
            "--disable-dev-shm-usage", 
            "--disable-software-rasterizer",
            "--disable-extensions",
            "--remote-allow-origins=*"
        );

        this.driver = new ChromeDriver(options);
    }

    @AfterEach
    public void tearDown()
    {
        if (this.driver != null) {
            this.driver.quit();
        }
    }

    /**
     * Testing the callback_registration_new.html page.
     * 
     * <p>System properties required:</p>
     * <ul>
     *   <li><b>patron</b>: The Aleph ID of the patron whose data will be used.</li>
     * </ul>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ./mvnw -Dtest=CallbackRegistrationNewTest -Dpatron=PREFIX12345 test
     * }</pre>
     */
    @Test
    public void testPageRender()
    {
        String patronAlephId = System.getProperty("patron");
        assertNotNull(patronAlephId, "The system property 'patron' must be set.");

        this.driver.get("http://localhost:" + this.port + "/bankid-registrator/test/callback_registration_new/" + patronAlephId);

        // // Log the HTML
        // String pageSource = this.driver.getPageSource();
        // logger.info("Page Source: \n{}", pageSource);

        WebElement firstName = this.driver.findElement(By.id("firstname"));
        WebElement lastName = this.driver.findElement(By.id("lastname"));

        assertThat(firstName).isNotNull();
        assertThat(lastName).isNotNull();
    }
}

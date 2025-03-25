package cz.cas.lib.bankid_registrator.selenium;

import cz.cas.lib.bankid_registrator.LdapServiceTest;
import cz.cas.lib.bankid_registrator.controllers.TestMainController;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Selenium test for the {@code callback_registration_new.html} page.
 * 
 * <p>System properties required:</p>
 * <ul>
 *  <li><b>patronId</b>: The Aleph ID of the patron whose data will be used.</li>
 * </ul>
 * 
 * <p>Example usage with `test.properties`:</p>
 * <pre>{@code
 * ./mvnw test -Dtest=CallbackRegistrationNewTest
 * }</pre>
 * or in order to override the properties:
 * <pre>{@code
 * ./mvnw test -Dtest=CallbackRegistrationNewTest -Dpatron=LIB000001
 * }</pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestMainController.class)
public class CallbackRegistrationNewTest
{
    @LocalServerPort
    private int port;

    private WebDriver driver;

    private static String patronId;

    private static final Logger logger = LoggerFactory.getLogger(CallbackRegistrationNewTest.class);

    @BeforeAll
    static void loadProperties() {
        Properties properties = new Properties();

        try (FileInputStream fis = new FileInputStream("src/test/resources/tests.properties")) {
            properties.load(fis);
        } catch (IOException e) {
            logger.warn("Failed to load test properties from 'tests.properties'.", e);
        }

        CallbackRegistrationNewTest.patronId = System.getProperty("patronId", properties.getProperty("CallbackRegistrationNewTest.patronId"));
    }

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
     * Testing the {@code callback_registration_new.html} page.
     * 
     * <p>System properties required:</p>
     * <ul>
     *   <li><b>patronId</b>: The Aleph ID of the patron whose data will be used.</li>
     * </ul>
     *
     * <p>Example usage with `test.properties`:</p>
     * <pre>{@code
     * ./mvnw test -Dtest=CallbackRegistrationNewTest#testPageRender
     * }</pre>
     * or in order to override the properties:
     * <pre>{@code
     * ./mvnw test -Dtest=CallbackRegistrationNewTest#testPageRender -DpatronId=LIB000001
     * }</pre>
     */
    @Test
    public void testPageRender()
    {
        String patronAlephId = CallbackRegistrationNewTest.patronId;
        assertNotNull(patronAlephId, "The system property 'patronId' must be set.");

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

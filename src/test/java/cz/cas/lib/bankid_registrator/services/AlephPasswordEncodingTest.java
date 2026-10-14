package cz.cas.lib.bankid_registrator.services;

import com.sun.net.httpserver.HttpServer;
import cz.cas.lib.bankid_registrator.configurations.MainConfiguration;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.HttpMethod;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AlephPasswordEncodingTest {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void passwordsSurviveXmlAndActualFormTransport(boolean rewriteHeader) throws Exception {
        MainConfiguration config = mock(MainConfiguration.class);
        when(config.getRewrite_aleph_batch_xml_header()).thenReturn(rewriteHeader);
        AlephService service = new AlephService(config, null, null, null, new DefaultResourceLoader());
        String oldPassword = "Old&<+%26 Č";
        String newPassword = "PepiCek123&€: +%26<>'\"";
        Map<String, String> generated = service.createPatronPswUpdateXml("TEST123", oldPassword, newPassword);
        assertFalse(generated.containsKey("error"), () -> generated.toString());
        String xml = generated.get("xml");
        assertPasswords(xml, oldPassword, newPassword);

        AtomicReference<String> receivedBody = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> method = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/password", exchange -> {
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            method.set(exchange.getRequestMethod());
            byte[] response = "<reply><reply-code>0000</reply-code></reply>".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (java.io.OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        try {
            Map<String, Object> result = service.doHttpRequest(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/password", HttpMethod.POST, xml);
            assertFalse(result.containsKey("error"), () -> result.toString());
            assertTrue(result.containsKey("response"));
            assertEquals("POST", method.get());
            assertTrue(contentType.get().startsWith("application/x-www-form-urlencoded"));
            String[] fields = receivedBody.get().split("&");
            assertEquals(1, fields.length, "XML must remain a single form field");
            String[] field = fields[0].split("=", 2);
            assertEquals("post_xml", field[0]);
            String decodedXml = URLDecoder.decode(field[1], StandardCharsets.UTF_8);
            assertEquals(xml, decodedXml, "One form decode must reproduce the original XML exactly");
            assertPasswords(decodedXml, oldPassword, newPassword);
        } finally {
            server.stop(0);
        }
    }

    private void assertPasswords(String xml, String oldPassword, String newPassword) throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));
        assertEquals(oldPassword, document.getElementsByTagName("old-password").item(0).getTextContent());
        assertEquals(newPassword, document.getElementsByTagName("new-password").item(0).getTextContent());
    }
}

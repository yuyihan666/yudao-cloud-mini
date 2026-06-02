package cn.iocoder.yudao.module.system.framework.socialauth.core;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultSocialAuthHttpClientTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void getShouldSendEncodedQueryParameters() {
        server.createContext("/get", exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            assertEquals("土豆", query.get("name"));
            assertEquals("https://app.example.com/callback", query.get("redirect_uri"));
            writeResponse(exchange, "get-ok");
        });
        DefaultSocialAuthHttpClient client = new DefaultSocialAuthHttpClient();

        String response = client.get(baseUrl + "/get", Map.of(
                "name", "土豆",
                "redirect_uri", "https://app.example.com/callback"
        ));

        assertEquals("get-ok", response);
    }

    @Test
    void postFormShouldSendEncodedFormBody() {
        server.createContext("/post", exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            assertEquals("application/x-www-form-urlencoded;charset=UTF-8", exchange.getRequestHeaders()
                    .getFirst("Content-Type"));
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> form = parseQuery(body);
            assertEquals("code-1", form.get("code"));
            assertEquals("https://app.example.com/callback", form.get("redirect_uri"));
            writeResponse(exchange, "post-ok");
        });
        DefaultSocialAuthHttpClient client = new DefaultSocialAuthHttpClient();

        String response = client.postForm(baseUrl + "/post", Map.of(
                "code", "code-1",
                "redirect_uri", "https://app.example.com/callback"
        ));

        assertEquals("post-ok", response);
    }

    private static Map<String, String> parseQuery(String query) {
        return Arrays.stream(query.split("&"))
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(part -> decode(part[0]), part -> decode(part[1])));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void writeResponse(HttpExchange exchange, String response) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

}

package ar.edu.utn.donatrack.gateway;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {
  static final HttpServer upstream;

  static {
    try {
      upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      upstream.createContext(
          "/",
          e -> {
            String output =
                e.getRequestMethod()
                    + " "
                    + e.getRequestURI()
                    + " "
                    + new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
                    + " "
                    + e.getRequestHeaders().getFirst("X-Trace-Id");
            byte[] body = output.getBytes(StandardCharsets.UTF_8);
            e.sendResponseHeaders(
                e.getRequestURI().getPath().equals("/missing") ? 404 : 200, body.length);
            e.getResponseBody().write(body);
            e.close();
          });
      upstream.start();
    } catch (Exception ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  @LocalServerPort int port;

  @DynamicPropertySource
  static void urls(DynamicPropertyRegistry registry) {
    for (String property :
        java.util.List.of(
            "DONACIONES_URL",
            "DONADORES_ENTIDADES_URL",
            "LOGISTICA_URL",
            "INCENTIVOS_URL",
            "CONSULTAS_URL"))
      registry.add(property, () -> "http://127.0.0.1:" + upstream.getAddress().getPort());
  }

  @AfterAll
  static void shutdown() {
    upstream.stop(0);
  }

  @Test
  void allFiveRoutesPreserveMethodBodyQueryAndTrace() throws Exception {
    try (HttpClient client = HttpClient.newHttpClient()) {
      for (String prefix :
          java.util.List.of(
              "donaciones", "donadores-entidades", "logistica", "incentivos", "consultas")) {
        var request =
            HttpRequest.newBuilder(
                    URI.create(
                        "http://127.0.0.1:" + port + "/api/" + prefix + "/resource?limite=3"))
                .timeout(java.time.Duration.ofSeconds(10))
                .header("X-Trace-Id", "demo-trace")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"cantidad\":3}"))
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("POST /resource?limite=3 {\"cantidad\":3} demo-trace", response.body());
        assertEquals("demo-trace", response.headers().firstValue("X-Trace-Id").orElseThrow());
      }
    }
  }

  @Test
  void preservesNotFoundAndReplacesInvalidTrace() throws Exception {
    try (HttpClient client = HttpClient.newHttpClient()) {
      var response =
          client.send(
              HttpRequest.newBuilder(
                      URI.create("http://127.0.0.1:" + port + "/api/consultas/missing"))
                  .header("X-Trace-Id", "invalid trace")
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(404, response.statusCode());
      assertNotEquals("invalid trace", response.headers().firstValue("X-Trace-Id").orElseThrow());
    }
  }
}

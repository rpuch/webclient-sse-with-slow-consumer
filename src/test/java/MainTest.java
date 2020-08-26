import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Schedulers;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

class MainTest {
    private TestServer server;

    @BeforeEach
    void init() throws Exception {
        server = new TestServer();
    }

    @AfterEach
    void cleanup() throws Exception {
        server.shutdown();
    }

    @Test
    void test() {
        WebClient client = WebClient.builder().build();

        client.get()
                .uri("http://localhost:9999/")
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                })
                .map(ServerSentEvent::data)
                .publishOn(Schedulers.single())
                .blockLast();
    }

    private static class TestServer {
        private final Server server;

        private TestServer() throws Exception {
            server = new Server(9999);
            server.setHandler(new AbstractHandler() {
                @Override
                public void handle(String target, Request request, HttpServletRequest httpServletRequest,
                        HttpServletResponse httpServletResponse) throws IOException, ServletException {
                    PrintWriter writer = httpServletResponse.getWriter();
                    for (int i = 0; i < 1_000_000; i++) {
                        writer.println("id:" + i);
                        writer.println("data:" + i);
                        writer.println();
                    }

                    request.setHandled(true);
                }
            });
            server.start();
        }

        void shutdown() throws Exception {
            server.stop();
        }
    }
}

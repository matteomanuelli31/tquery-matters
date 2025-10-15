import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.DocumentContext;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class JsonPathServer {
    private final DocumentContext json;

    public JsonPathServer(String dataPath) throws Exception {
        json = JsonPath.parse(Files.readString(Paths.get(dataPath)));
    }

    public void start(int port) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/filterProjects", this::handle);
        server.start();
    }

    private void handle(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            DocumentContext req = JsonPath.parse(is);
            String status = req.read("$.status");
            String technology = req.read("$.technology");

            String query = String.format("$..projects[?(@.status == '%s' && '%s' in @.technologies)]",
                status, technology);
            List<?> results = json.read(query, List.class);

            String resultsJson = JsonPath.parse(results).jsonString();
            String response = String.format("{\"count\":%d,\"results\":%s}", results.size(), resultsJson);
            ex.sendResponseHeaders(200, response.length());
            ex.getResponseBody().write(response.getBytes());
        } catch (Exception e) {
            ex.sendResponseHeaders(500, -1);
        } finally {
            ex.close();
        }
    }

    public static void main(String[] args) throws Exception {
        new JsonPathServer("large_data.json").start(9000);
    }
}

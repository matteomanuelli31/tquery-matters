import com.jayway.jsonpath.JsonPath;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class TestFilter {
    public static void main(String[] args) throws Exception {
        String json = Files.readString(Paths.get("test_data.json"));

        // Filter 1: matches first entry (Alice with Laptop)
        String query1 = "$.entries[?(@.tree1.details.age > 28 && @.tree2.specs.price > 1000)]";
        List<?> results1 = JsonPath.read(json, query1);
        System.out.println("Filter 1 results: " + results1.size());
        System.out.println(JsonPath.parse(results1).jsonString());

        System.out.println();

        // Filter 2: matches second entry (Bob with Phone)
        String query2 = "$.entries[?(@.tree1.details.age < 28 && @.tree2.specs.price < 1000)]";
        List<?> results2 = JsonPath.read(json, query2);
        System.out.println("Filter 2 results: " + results2.size());
        System.out.println(JsonPath.parse(results2).jsonString());
    }
}

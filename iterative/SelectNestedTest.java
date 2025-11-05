import java.util.List;

public class SelectNestedTest {
	public static void main(String[] args) {
		// Create structure: data[0].a.b = 6, data[1].a.b = 5
		ValueVector data = Value.create().getChildren("data");

		// data[0].a.b = 6
		Value item0 = data.get(0);
		item0.getChildren("a").first().getChildren("b").first().setValue(6);

		// data[1].a.b = 5
		Value item1 = data.get(1);
		item1.getChildren("a").first().getChildren("b").first().setValue(5);

		// Test: SELECT $.a FROM data WHERE .b = 5
		System.out.println("=== Test: SELECT $.a FROM data WHERE .b = 5 ===");
		System.out.println("Structure:");
		System.out.println("  data[0].a.b = 6");
		System.out.println("  data[1].a.b = 5");

		List<String> results = new SelectBuilder()
			.select("$.a")
			.from(data, "data")
			.where(".b = 5")
			.exec();

		System.out.println("\nResults:");
		for (String path : results) {
			System.out.println("  " + path);
		}

		boolean test = results.size() == 1 && results.contains("data[1].a");
		System.out.println(test ? "\n✓ Test PASSED" : "\n✗ Test FAILED");

		System.exit(test ? 0 : 1);
	}
}

import java.util.List;

public class TestArrayCheck {
	public static void main(String[] args) {
		Value root = Value.create();

		// Create structure with tags array
		Value item = root.getChildren("item").first();
		ValueVector tags = item.getChildren("tags");
		tags.get(0).setValue("admin");
		tags.get(1).setValue("user");
		tags.get(2).setValue("admin");  // Another admin

		System.out.println("=== Test: SELECT $.item WHERE ..tags[*] = admin ===");
		System.out.println("Structure: item.tags[0]=admin, tags[1]=user, tags[2]=admin");

		List<String> results = new SelectBuilder()
			.select("$.item")
			.from(root, "root")
			.where("..tags[*] = admin")
			.exec();

		System.out.println("Results: " + results);
		System.out.println("Expected: [root.item]");

		if (results.equals(List.of("root.item"))) {
			System.out.println("✓ PASS - Found item because it has at least one tag=admin");
		} else {
			System.out.println("✗ FAIL");
		}

		// Test with no match
		System.out.println("\n=== Test: SELECT $.item WHERE ..tags[*] = moderator ===");
		List<String> results2 = new SelectBuilder()
			.select("$.item")
			.from(root, "root")
			.where("..tags[*] = moderator")
			.exec();

		System.out.println("Results: " + results2);
		System.out.println("Expected: []");
		System.out.println(results2.isEmpty() ? "✓ PASS" : "✗ FAIL");
	}
}

import java.util.List;

public class TestFirstCheck {
	public static void main(String[] args) {
		Value root = Value.create();
		Value item = root.getChildren("item").first();
		item.getChildren("tag").first().setValue("admin");

		// Test 1: Non-array descendant pattern
		System.out.println("=== Test 1: SELECT $..tag WHERE ..tag = admin ===");
		System.out.println("(Non-array pattern: ..tag)");
		List<String> test1 = new SelectBuilder()
			.select("$..tag")
			.from(root, "root")
			.where("..tag = admin")
			.exec();
		System.out.println("Results: " + test1);
		System.out.println("Expected: [root.item.tag]");
		System.out.println();

		// Test 2: What happens if we're at "tag" and check for ".tag"?
		Value root2 = Value.create();
		root2.getChildren("tag").first().setValue("admin");

		System.out.println("=== Test 2: SELECT $.tag WHERE ..tag = admin ===");
		System.out.println("(Already at tag field, checking for descendant tag)");
		List<String> test2 = new SelectBuilder()
			.select("$.tag")
			.from(root2, "root")
			.where("..tag = admin")
			.exec();
		System.out.println("Results: " + test2);
		System.out.println("Expected: [root.tag] - should match itself");
		System.out.println();

		// Test 3: Array pattern - first check should NOT trigger
		Value root3 = Value.create();
		ValueVector tags = root3.getChildren("item").first().getChildren("tags");
		tags.get(0).setValue("admin");
		tags.get(1).setValue("user");

		System.out.println("=== Test 3: SELECT $..tags[*] WHERE ..tags[*] = admin ===");
		System.out.println("(Array pattern: ..tags[*] - first check should NOT trigger)");
		List<String> test3 = new SelectBuilder()
			.select("$..tags[*]")
			.from(root3, "root")
			.where("..tags[*] = admin")
			.exec();
		System.out.println("Results: " + test3);
		System.out.println("Expected: [root.item.tags[0]] - only admin element");
	}
}

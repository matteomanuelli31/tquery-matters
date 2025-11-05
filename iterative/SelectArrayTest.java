import java.util.List;

public class SelectArrayTest {
	public static void main(String[] args) {
		// Create test structure - use ValueVector directly
		ValueVector items = Value.create().getChildren("items");

		// items[0]: status="active", type="user", priority=1
		Value item0 = items.get(0);
		item0.getChildren("status").first().setValue("active");
		item0.getChildren("type").first().setValue("user");
		item0.getChildren("priority").first().setValue(1);

		// items[1]: status="inactive", type="user", priority=2
		Value item1 = items.get(1);
		item1.getChildren("status").first().setValue("inactive");
		item1.getChildren("type").first().setValue("user");
		item1.getChildren("priority").first().setValue(2);

		// items[2]: status="active", type="admin", priority=3
		Value item2 = items.get(2);
		item2.getChildren("status").first().setValue("active");
		item2.getChildren("type").first().setValue("admin");
		item2.getChildren("priority").first().setValue(3);

		// items[3]: status="suspended", type="user", priority=1
		Value item3 = items.get(3);
		item3.getChildren("status").first().setValue("suspended");
		item3.getChildren("type").first().setValue("user");
		item3.getChildren("priority").first().setValue(1);

		// Test 1: Simple condition
		System.out.println("=== Test 1: WHERE .status = active ===");
		List<String> results1 = new SelectBuilder()
			.select("$")
			.from(items, "items")
			.where(".status = active")
			.exec();

		System.out.println("Results:");
		for (String path : results1) {
			System.out.println("  " + path);
		}
		boolean test1 = results1.size() == 2 &&
			results1.contains("items[0]") &&
			results1.contains("items[2]");
		System.out.println(test1 ? "✓ Test 1 PASSED" : "✗ Test 1 FAILED");

		// Test 2: NOT operator
		System.out.println("\n=== Test 2: WHERE !.status = active ===");
		List<String> results2 = new SelectBuilder()
			.select("$")
			.from(items, "items")
			.where("!.status = active")
			.exec();

		System.out.println("Results:");
		for (String path : results2) {
			System.out.println("  " + path);
		}
		boolean test2 = results2.size() == 2 &&
			results2.contains("items[1]") &&
			results2.contains("items[3]");
		System.out.println(test2 ? "✓ Test 2 PASSED" : "✗ Test 2 FAILED");

		// Test 3: AND operator
		System.out.println("\n=== Test 3: WHERE .status = active && .type = user ===");
		List<String> results3 = new SelectBuilder()
			.select("$")
			.from(items, "items")
			.where(".status = active && .type = user")
			.exec();

		System.out.println("Results:");
		for (String path : results3) {
			System.out.println("  " + path);
		}
		boolean test3 = results3.size() == 1 &&
			results3.contains("items[0]");
		System.out.println(test3 ? "✓ Test 3 PASSED" : "✗ Test 3 FAILED");

		// Test 4: OR operator
		System.out.println("\n=== Test 4: WHERE .status = inactive || .status = suspended ===");
		List<String> results4 = new SelectBuilder()
			.select("$")
			.from(items, "items")
			.where(".status = inactive || .status = suspended")
			.exec();

		System.out.println("Results:");
		for (String path : results4) {
			System.out.println("  " + path);
		}
		boolean test4 = results4.size() == 2 &&
			results4.contains("items[1]") &&
			results4.contains("items[3]");
		System.out.println(test4 ? "✓ Test 4 PASSED" : "✗ Test 4 FAILED");

		// Test 5: AND + NOT
		System.out.println("\n=== Test 5: WHERE .status = active && !.type = admin ===");
		List<String> results5 = new SelectBuilder()
			.select("$")
			.from(items, "items")
			.where(".status = active && !.type = admin")
			.exec();

		System.out.println("Results:");
		for (String path : results5) {
			System.out.println("  " + path);
		}
		boolean test5 = results5.size() == 1 &&
			results5.contains("items[0]");
		System.out.println(test5 ? "✓ Test 5 PASSED" : "✗ Test 5 FAILED");

		// Test 6: Parentheses
		System.out.println("\n=== Test 6: WHERE (.type = user && .priority = 1) || .type = admin ===");
		List<String> results6 = new SelectBuilder()
			.select("$")
			.from(items, "items")
			.where("(.type = user && .priority = 1) || .type = admin")
			.exec();

		System.out.println("Results:");
		for (String path : results6) {
			System.out.println("  " + path);
		}
		boolean test6 = results6.size() == 3 &&
			results6.contains("items[0]") &&
			results6.contains("items[2]") &&
			results6.contains("items[3]");
		System.out.println(test6 ? "✓ Test 6 PASSED" : "✗ Test 6 FAILED");

		if (test1 && test2 && test3 && test4 && test5 && test6) {
			System.out.println("\n✓ ALL TESTS PASSED");
			System.exit(0);
		} else {
			System.out.println("\n✗ SOME TESTS FAILED");
			System.exit(1);
		}
	}
}

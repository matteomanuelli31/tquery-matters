import java.util.List;

public class SelectSimpleTest {
	public static void main(String[] args) {
		// Test 1: Simple path with WHERE
		System.out.println("=== Test 1: SELECT $.b.c FROM a WHERE . = 5 ===");
		Value a = Value.create();
		a.getChildren("b").first().getChildren("c").first().setValue(5);

		List<String> results1 = new SelectBuilder()
			.select("$.b.c")
			.from(a, "a")
			.where(". = 5")
			.exec();

		System.out.println("Results:");
		for (String path : results1) {
			System.out.println("  " + path);
		}
		boolean test1 = results1.size() == 1 && results1.contains("a.b.c");
		System.out.println(test1 ? "✓ Test 1 PASSED" : "✗ Test 1 FAILED");

		// Test 2: Wildcard
		System.out.println("\n=== Test 2: SELECT $.* FROM root WHERE . = 10 ===");
		Value root = Value.create();
		root.getChildren("x").first().setValue(10);
		root.getChildren("y").first().setValue(20);
		root.getChildren("z").first().setValue(10);

		List<String> results2 = new SelectBuilder()
			.select("$.*")
			.from(root, "root")
			.where(". = 10")
			.exec();

		System.out.println("Results:");
		for (String path : results2) {
			System.out.println("  " + path);
		}
		boolean test2 = results2.size() == 2 && results2.contains("root.x") && results2.contains("root.z");
		System.out.println(test2 ? "✓ Test 2 PASSED" : "✗ Test 2 FAILED");

		// Test 3: Descendant search
		System.out.println("\n=== Test 3: SELECT $..value FROM tree WHERE . = 42 ===");
		Value tree = Value.create();
		tree.getChildren("a").first().getChildren("value").first().setValue(42);
		tree.getChildren("b").first().getChildren("c").first().getChildren("value").first().setValue(42);

		List<String> results3 = new SelectBuilder()
			.select("$..value")
			.from(tree, "tree")
			.where(". = 42")
			.exec();

		System.out.println("Results:");
		for (String path : results3) {
			System.out.println("  " + path);
		}
		boolean test3 = results3.size() == 2 &&
			results3.contains("tree.a.value") &&
			results3.contains("tree.b.c.value");
		System.out.println(test3 ? "✓ Test 3 PASSED" : "✗ Test 3 FAILED");

		if (test1 && test2 && test3) {
			System.out.println("\n✓ ALL TESTS PASSED");
			System.exit(0);
		} else {
			System.out.println("\n✗ SOME TESTS FAILED");
			System.exit(1);
		}
	}
}

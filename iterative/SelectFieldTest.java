import java.util.List;

public class SelectFieldTest {
	public static void main(String[] args) {
		boolean allPass = true;

		// Test 1: Field existence with heterogeneous data AND other condition
		System.out.println("=== Test 1: .email in . && .age = 30 ===");
		ValueVector items = Value.create().getChildren("items");

		// items[0] has name, age=30, email
		Value item0 = items.get(0);
		item0.getChildren("name").first().setValue("Alice");
		item0.getChildren("age").first().setValue(30);
		item0.getChildren("email").first().setValue("alice@example.com");

		// items[1] has name, age=25, email
		Value item1 = items.get(1);
		item1.getChildren("name").first().setValue("Bob");
		item1.getChildren("age").first().setValue(25);
		item1.getChildren("email").first().setValue("bob@example.com");

		// items[2] has name, age=30 (no email)
		Value item2 = items.get(2);
		item2.getChildren("name").first().setValue("Charlie");
		item2.getChildren("age").first().setValue(30);

		// items[3] has only name and email
		Value item3 = items.get(3);
		item3.getChildren("name").first().setValue("David");
		item3.getChildren("email").first().setValue("david@example.com");

		List<String> result1 = new SelectBuilder()
			.select("$")
			.from(items, "items")
			.where(".email in . && .age = 30")
			.exec();

		System.out.println("Results: " + result1);
		System.out.println("Expected: [items[0]]");
		boolean test1 = result1.size() == 1 && result1.contains("items[0]");
		System.out.println(test1 ? "✓ PASS\n" : "✗ FAIL\n");
		allPass &= test1;

		// Test 2: Path traversal in WHERE clause .b.c = value
		System.out.println("=== Test 2: SELECT $.a FROM data WHERE .b.c = 5 ===");
		ValueVector data = Value.create().getChildren("data");

		// data[0].a has id=1 and b.c=6
		Value a0 = data.get(0).getChildren("a").first();
		a0.getChildren("id").first().setValue(1);
		a0.getChildren("b").first()
			.getChildren("c").first().setValue(6);

		// data[1].a has id=2 and b.c=5
		Value a1 = data.get(1).getChildren("a").first();
		a1.getChildren("id").first().setValue(2);
		a1.getChildren("b").first()
			.getChildren("c").first().setValue(5);

		// data[2].a has id=3, no b
		data.get(2).getChildren("a").first()
			.getChildren("id").first().setValue(3);

		// data[3].a has id=4, b exists but no c
		Value a3 = data.get(3).getChildren("a").first();
		a3.getChildren("id").first().setValue(4);
		a3.getChildren("b").first()
			.getChildren("x").first().setValue(10);

		List<String> result2 = new SelectBuilder()
			.select("$.a")
			.from(data, "data")
			.where(".b.c = 5")
			.exec();

		System.out.println("Results: " + result2);
		System.out.println("Expected: [data[1].a]");
		boolean test2 = result2.size() == 1 && result2.contains("data[1].a");
		System.out.println(test2 ? "✓ PASS\n" : "✗ FAIL\n");
		allPass &= test2;

		// Test 3: Nested field existence .settings.notifications in .
		System.out.println("=== Test 3: .settings.notifications in . (nested path existence) ===");
		ValueVector users = Value.create().getChildren("users");

		// users[0]: has settings.notifications
		users.get(0).getChildren("name").first().setValue("Alice");
		users.get(0).getChildren("settings").first()
			.getChildren("notifications").first()
			.getChildren("enabled").first().setValue(true);

		// users[1]: has settings but no notifications
		users.get(1).getChildren("name").first().setValue("Bob");
		users.get(1).getChildren("settings").first()
			.getChildren("theme").first().setValue("dark");

		// users[2]: no settings at all
		users.get(2).getChildren("name").first().setValue("Charlie");

		List<String> result3 = new SelectBuilder()
			.select("$")
			.from(users, "users")
			.where(".settings.notifications in .")
			.exec();

		System.out.println("Results: " + result3);
		System.out.println("Expected: [users[0]]");
		boolean test3 = result3.size() == 1 && result3.contains("users[0]");
		System.out.println(test3 ? "✓ PASS\n" : "✗ FAIL\n");
		allPass &= test3;

		// Test 4: .technologies (single dot, not descendant) with value check
		System.out.println("=== Test 4: .status = active && .technologies = Python ===");
		ValueVector projects = Value.create().getChildren("projects");

		// projects[0]: status=active, technologies=Python
		Value proj0 = projects.get(0);
		proj0.getChildren("status").first().setValue("active");
		proj0.getChildren("technologies").first().setValue("Python");

		// projects[1]: status=active, technologies=Java
		Value proj1 = projects.get(1);
		proj1.getChildren("status").first().setValue("active");
		proj1.getChildren("technologies").first().setValue("Java");

		// projects[2]: status=inactive, technologies=Python
		Value proj2 = projects.get(2);
		proj2.getChildren("status").first().setValue("inactive");
		proj2.getChildren("technologies").first().setValue("Python");

		// projects[3]: status=active, no technologies
		Value proj3 = projects.get(3);
		proj3.getChildren("status").first().setValue("active");

		List<String> result4 = new SelectBuilder()
			.select("$")
			.from(projects, "projects")
			.where(".status = active && .technologies = Python")
			.exec();

		System.out.println("Results: " + result4);
		System.out.println("Expected: [projects[0]]");
		boolean test4 = result4.size() == 1 && result4.contains("projects[0]");
		System.out.println(test4 ? "✓ PASS\n" : "✗ FAIL\n");
		allPass &= test4;

		// Test 5: Multi-level path traversal .user.settings.theme = dark
		System.out.println("=== Test 5: .user.settings.theme = dark ===");
		ValueVector accounts = Value.create().getChildren("accounts");

		// accounts[0]: user.settings.theme=dark
		accounts.get(0).getChildren("user").first()
			.getChildren("settings").first()
			.getChildren("theme").first()
			.setValue("dark");

		// accounts[1]: user.settings.theme=light
		accounts.get(1).getChildren("user").first()
			.getChildren("settings").first()
			.getChildren("theme").first()
			.setValue("light");

		// accounts[2]: user.settings exists but no theme
		accounts.get(2).getChildren("user").first()
			.getChildren("settings").first()
			.getChildren("language").first()
			.setValue("en");

		List<String> result5 = new SelectBuilder()
			.select("$")
			.from(accounts, "accounts")
			.where(".user.settings.theme = dark")
			.exec();

		System.out.println("Results: " + result5);
		System.out.println("Expected: [accounts[0]]");
		boolean test5 = result5.size() == 1 && result5.contains("accounts[0]");
		System.out.println(test5 ? "✓ PASS\n" : "✗ FAIL\n");
		allPass &= test5;

		// Summary
		if (allPass) {
			System.out.println("✓ ALL TESTS PASSED");
			System.exit(0);
		} else {
			System.out.println("✗ SOME TESTS FAILED");
			System.exit(1);
		}
	}
}

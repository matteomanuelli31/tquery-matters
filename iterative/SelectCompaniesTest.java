import java.io.FileReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class SelectCompaniesTest {
	public static void main(String[] args) {
		try {
			// Load and parse companies.json
			Value root = Value.create();
			JsonUtils.parseJsonIntoValue(new FileReader("companies.json"), root, false);

			ValueVector companies = root.getChildren("companies");

			System.out.println("Loaded " + companies.size() + " companies\n");

			// Test 1: AND - in_progress AND Python
			System.out.println("=== Test 1: in_progress AND Python ===");
			List<String> test1Paths = new SelectBuilder()
				.select("$..projects[*]")
				.from(companies, "companies")
				.where(".status = in_progress && ..technologies[*] = Python")
				.exec();

			List<String> test1Ids = extractIds(companies, test1Paths);
			System.out.println("Found " + test1Ids.size() + " projects:");
			test1Ids.forEach(id -> System.out.println("  " + id));

			List<String> expected1 = Arrays.asList("P001", "P202", "P600", "P801");

			boolean pass1 = test1Ids.size() == expected1.size() &&
				test1Ids.containsAll(expected1) && expected1.containsAll(test1Ids);
			System.out.println(pass1 ? "✓ Test 1 PASSED" : "✗ Test 1 FAILED");

			// Test 2: OR - in_progress OR completed
			System.out.println("\n=== Test 2: in_progress OR completed ===");
			List<String> test2Paths = new SelectBuilder()
				.select("$..projects[*]")
				.from(companies, "companies")
				.where(".status = in_progress || .status = completed")
				.exec();

			List<String> test2Ids = extractIds(companies, test2Paths);
			System.out.println("Found " + test2Ids.size() + " projects:");
			test2Ids.forEach(id -> System.out.println("  " + id));

			// 10 in_progress + 7 completed = 17
			boolean pass2 = test2Ids.size() == 17 &&
				test2Ids.contains("P001") && test2Ids.contains("P004");
			System.out.println(pass2 ? "✓ Test 2 PASSED" : "✗ Test 2 FAILED");

			// Test 3: NOT - NOT completed
			System.out.println("\n=== Test 3: NOT completed ===");
			List<String> test3Paths = new SelectBuilder()
				.select("$..projects[*]")
				.from(companies, "companies")
				.where("!.status = completed")
				.exec();

			List<String> test3Ids = extractIds(companies, test3Paths);
			System.out.println("Found " + test3Ids.size() + " projects (not completed):");
			test3Ids.forEach(id -> System.out.println("  " + id));

			// Total=33, completed=7, so not completed=26
			boolean pass3 = test3Ids.size() == 26 &&
				!test3Ids.contains("P004") && // completed
				test3Ids.contains("P001");    // in_progress
			System.out.println(pass3 ? "✓ Test 3 PASSED" : "✗ Test 3 FAILED");

			// Test 4: Complex - (in_progress AND Python) OR (completed AND Java)
			System.out.println("\n=== Test 4: (in_progress AND Python) OR (completed AND Java) ===");
			List<String> test4Paths = new SelectBuilder()
				.select("$..projects[*]")
				.from(companies, "companies")
				.where("(.status = in_progress && ..technologies[*] = Python) || (.status = completed && ..technologies[*] = Java)")
				.exec();

			List<String> test4Ids = extractIds(companies, test4Paths);
			System.out.println("Found " + test4Ids.size() + " projects:");
			test4Ids.forEach(id -> System.out.println("  " + id));

			List<String> expected4 = Arrays.asList("P001", "P202", "P600", "P801");

			boolean pass4 = test4Ids.size() == expected4.size() &&
				test4Ids.containsAll(expected4) && expected4.containsAll(test4Ids);
			System.out.println(pass4 ? "✓ Test 4 PASSED" : "✗ Test 4 FAILED");

			if (pass1 && pass2 && pass3 && pass4) {
				System.out.println("\n✓ ALL TESTS PASSED");
				System.exit(0);
			} else {
				System.out.println("\n✗ SOME TESTS FAILED");
				System.exit(1);
			}

		} catch (Exception e) {
			System.err.println("✗ Failed: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	// Extract project_id values from paths
	private static List<String> extractIds(ValueVector companies, List<String> paths) {
		List<String> ids = new ArrayList<>();
		for (String path : paths) {
			Value projectNode = navigatePath(companies, path);
			if (projectNode != null && projectNode.hasChildren("project_id")) {
				Value idValue = projectNode.getChildren("project_id").first();
				if (idValue.isDefined()) {
					ids.add(idValue.strValue());
				}
			}
		}
		return ids;
	}

	// Navigate a path string like "companies[0].company.departments[1].teams[0].projects[0]"
	private static Value navigatePath(ValueVector companies, String path) {
		String[] parts = path.split("\\.");
		Value current = null;

		for (String part : parts) {
			if (part.isEmpty()) continue;

			int bracketIndex = part.indexOf('[');
			if (bracketIndex != -1) {
				String fieldName = part.substring(0, bracketIndex);
				int endBracket = part.indexOf(']');
				int index = Integer.parseInt(part.substring(bracketIndex + 1, endBracket));

				if (fieldName.equals("companies")) {
					// Start from companies array
					if (index >= companies.size()) return null;
					current = companies.get(index);
				} else {
					// Navigate to field and get array element
					if (current == null || !current.hasChildren(fieldName)) return null;
					ValueVector vec = current.getChildren(fieldName);
					if (vec.isEmpty() || index >= vec.size()) return null;
					current = vec.get(index);
				}
			} else {
				// No array index - get first element (a.b = a.b[0])
				if (current == null || !current.hasChildren(part)) return null;
				ValueVector vec = current.getChildren(part);
				if (vec.isEmpty()) return null;
				current = vec.first();
			}
		}

		return current;
	}
}

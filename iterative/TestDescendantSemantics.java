import java.util.List;

public class TestDescendantSemantics {
	public static void main(String[] args) {
		Value root = Value.create();

		// Create structure: root.project.technologies[0]="Python", [1]="Java", [2]="Go"
		Value project = root.getChildren("project").first();
		ValueVector techs = project.getChildren("technologies");
		techs.get(0).setValue("Python");
		techs.get(1).setValue("Java");
		techs.get(2).setValue("Go");

		// Test 1: SELECT project, WHERE descendant has Python
		System.out.println("=== Test 1: SELECT $.project WHERE ..technologies[*] = Python ===");
		List<String> test1 = new SelectBuilder()
			.select("$.project")
			.from(root, "root")
			.where("..technologies[*] = Python")
			.exec();
		System.out.println("Results: " + test1);
		System.out.println("Expected: [root.project]");
		System.out.println(test1.equals(List.of("root.project")) ? "✓ PASS\n" : "✗ FAIL\n");

		// Test 2: SELECT technologies array elements, WHERE descendant check
		System.out.println("=== Test 2: SELECT $.project.technologies[*] WHERE ..technologies[*] = Python ===");
		List<String> test2 = new SelectBuilder()
			.select("$.project.technologies[*]")
			.from(root, "root")
			.where("..technologies[*] = Python")
			.exec();
		System.out.println("Results: " + test2);
		System.out.println("Expected: ??? (what should this return?)");
		System.out.println("  - If checking 'do I have descendants with technologies=Python': NONE (leaf nodes)");
		System.out.println("  - If checking 'am I part of technologies with Python present': ALL (Python, Java, Go)");
		System.out.println("  - Current implementation: " + (test2.size() == 1 ? "ONLY Python" : "size=" + test2.size()));

		// Test 3: SELECT technologies, WHERE own value
		System.out.println("\n=== Test 3: SELECT $.project.technologies[*] WHERE . = Python ===");
		List<String> test3 = new SelectBuilder()
			.select("$.project.technologies[*]")
			.from(root, "root")
			.where(". = Python")
			.exec();
		System.out.println("Results: " + test3);
		System.out.println("Expected: [root.project.technologies[0]]");
		System.out.println(test3.equals(List.of("root.project.technologies[0]")) ? "✓ PASS" : "✗ FAIL");
	}
}

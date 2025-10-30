import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;

import java.util.List;
import java.util.Map;

public class RunTests {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║       JSONPath Exploration Tests                           ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        runTest("testBasicPathSelection", RunTests::testBasicPathSelection);
        runTest("testWildcardWithFilter", RunTests::testWildcardWithFilter);
        runTest("testRecursiveDescent", RunTests::testRecursiveDescent);
        runTest("testCanWeGetPaths", RunTests::testCanWeGetPaths);
        runTest("testExactDepthMatching", RunTests::testExactDepthMatching);
        runTest("testMultipleLevelWildcard", RunTests::testMultipleLevelWildcard);
        runTest("testSelectParentFilterByDescendant", RunTests::testSelectParentFilterByDescendant);
        runTest("testMultipleConditions_AllMustMatch", RunTests::testMultipleConditions_AllMustMatch);
        runTest("testMultipleConditions_WithRecursiveDescent", RunTests::testMultipleConditions_WithRecursiveDescent);
        runTest("testNonHomogeneous_MixedStructures", RunTests::testNonHomogeneous_MixedStructures);
        runTest("testSelectParentCollection_IfChildrenMatch", RunTests::testSelectParentCollection_IfChildrenMatch);
        runTest("testSelectParentCollection_GetPaths", RunTests::testSelectParentCollection_GetPaths);
        runTest("testArrayContains_InOperator", RunTests::testArrayContains_InOperator);
        runTest("testDescendantFilter_FindObjectsAtAnyDepth", RunTests::testDescendantFilter_FindObjectsAtAnyDepth);
        runTest("testReturnSingleCollection_IfNestedConditionMatches", RunTests::testReturnSingleCollection_IfNestedConditionMatches);

        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println(String.format("║  PASSED: %-3d  FAILED: %-3d                              ║", passed, failed));
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }

    static void runTest(String name, Runnable test) {
        try {
            test.run();
            System.out.println("✓ PASSED: " + name);
            passed++;
        } catch (Exception e) {
            System.out.println("✗ FAILED: " + name);
            System.out.println("  Error: " + e.getMessage());
            failed++;
        }
    }

    static void testBasicPathSelection() {
        Map<String, Object> data = Map.of("a", Map.of("b", Map.of("c", "ciao")));
        String result = JsonPath.read(data, "$.a.b.c");
        if (!"ciao".equals(result)) throw new AssertionError("Expected 'ciao', got: " + result);
    }

    static void testWildcardWithFilter() {
        Map<String, Object> data = Map.of(
            "a", Map.of(
                "item1", Map.of("d", 5, "name", "first"),
                "item2", Map.of("d", 10, "name", "second"),
                "item3", Map.of("d", 5, "name", "third")
            )
        );
        List<Map<String, Object>> results = JsonPath.read(data, "$.a.*[?(@.d == 5)]");
        System.out.println("  → Found " + results.size() + " items with d==5");
        if (results.size() != 2) throw new AssertionError("Expected 2, got: " + results.size());
    }

    static void testRecursiveDescent() {
        Map<String, Object> data = Map.of(
            "a", Map.of(
                "d", 5,
                "b", Map.of("d", 10),
                "x", Map.of("y", Map.of("d", 15))
            )
        );
        List<Integer> results = JsonPath.read(data, "$.a..d");
        System.out.println("  → $.a..d found: " + results);
        if (results.size() != 3) throw new AssertionError("Expected 3 values");
    }

    static void testCanWeGetPaths() {
        Map<String, Object> data = Map.of(
            "a", Map.of(
                "item1", Map.of("d", 5),
                "item2", Map.of("d", 10),
                "item3", Map.of("d", 5)
            )
        );
        Configuration conf = Configuration.builder().options(Option.AS_PATH_LIST).build();
        List<String> paths = JsonPath.using(conf).parse(data).read("$.a.*[?(@.d == 5)]");
        System.out.println("  → Paths: " + paths);
        if (paths.size() != 2) throw new AssertionError("Expected 2 paths");
    }

    static void testExactDepthMatching() {
        Map<String, Object> data = Map.of(
            "a", Map.of(
                "d", 5,
                "b", Map.of("d", 5),
                "x", Map.of("y", Map.of("d", 5))
            )
        );
        // $.a.*.d should match only a.b.d (not a.d, not a.x.y.d)
        List<Integer> results = JsonPath.read(data, "$.a.*.d");
        Configuration conf = Configuration.builder().options(Option.AS_PATH_LIST).build();
        List<String> paths = JsonPath.using(conf).parse(data).read("$.a.*.d");

        System.out.println("  → $.a.*.d (1 level deep): " + results.size() + " result(s) at " + paths);

        if (results.size() != 1) throw new AssertionError("Expected 1 (only a.b.d)");
        if (!results.get(0).equals(5)) throw new AssertionError("Expected value 5");
        if (!paths.get(0).equals("$['a']['b']['d']")) throw new AssertionError("Expected path $['a']['b']['d']");
    }

    static void testMultipleLevelWildcard() {
        Map<String, Object> data = Map.of("a", Map.of("x", Map.of("y", Map.of("d", 5))));
        // $.a.*.*.d should match a.x.y.d (exactly 2 levels deep)
        List<Integer> results = JsonPath.read(data, "$.a.*.*.d");
        Configuration conf = Configuration.builder().options(Option.AS_PATH_LIST).build();
        List<String> paths = JsonPath.using(conf).parse(data).read("$.a.*.*.d");

        System.out.println("  → $.a.*.*.d (2 levels deep): " + results.size() + " result(s) at " + paths);

        if (results.size() != 1) throw new AssertionError("Expected 1");
        if (results.get(0) != 5) throw new AssertionError("Expected value 5");
        if (!paths.get(0).equals("$['a']['x']['y']['d']")) throw new AssertionError("Expected path $['a']['x']['y']['d']");
    }

    static void testSelectParentFilterByDescendant() {
        Map<String, Object> data = Map.of(
            "a", List.of(
                Map.of("b", Map.of("c", Map.of("d", Map.of("e", 5)))),
                Map.of("b", Map.of("c", Map.of("d", Map.of("e", 10)))),
                Map.of("b", Map.of("c", Map.of("d", Map.of("e", 5))))
            )
        );
        List<Map<String, Object>> results = JsonPath.read(data, "$.a[?(@.b.c.d.e == 5)].b");
        System.out.println("  → Selected " + results.size() + " 'b' objects where e==5");
        if (results.size() != 2) throw new AssertionError("Expected 2");
    }

    static void testMultipleConditions_AllMustMatch() {
        Map<String, Object> data = Map.of(
            "items", List.of(
                Map.of("id", 1, "status", "active", "data", Map.of("d", 5, "nested", Map.of("e", 10))),
                Map.of("id", 2, "status", "inactive", "data", Map.of("d", 5, "nested", Map.of("e", 10))),
                Map.of("id", 3, "status", "active", "data", Map.of("d", 10, "nested", Map.of("e", 10))),
                Map.of("id", 4, "status", "active", "data", Map.of("d", 5, "nested", Map.of("e", 20))),
                Map.of("id", 5, "status", "active", "data", Map.of("d", 5, "nested", Map.of("e", 10)))
            )
        );
        List<Map<String, Object>> results = JsonPath.read(data,
            "$.items[?(@.status == 'active' && @.data.d == 5 && @.data.nested.e == 10)]");
        System.out.println("  → AND filter matched: " + results.size() + " items (ids: " +
            results.stream().map(m -> m.get("id")).toList() + ")");
        if (results.size() != 2) throw new AssertionError("Expected 2 (ids 1 and 5)");
    }

    static void testMultipleConditions_WithRecursiveDescent() {
        Map<String, Object> data = Map.of(
            "items", List.of(
                Map.of("id", 1, "d", 5, "x", Map.of("e", 10)),
                Map.of("id", 2, "e", 10, "y", Map.of("d", 5)),
                Map.of("id", 3, "d", 5, "other", "value")
            )
        );
        // Using 'in' operator with recursive descent works!
        List<Map<String, Object>> results = JsonPath.read(data, "$.items[?(5 in @..d && 10 in @..e)]");
        System.out.println("  → Recursive 'value in @..field' syntax: " + results.size() + " items");
        if (results.size() != 2) throw new AssertionError("Expected 2 (ids 1, 2)");
    }

    static void testNonHomogeneous_MixedStructures() {
        Map<String, Object> data = Map.of(
            "items", List.of(
                Map.of("name", "item1", "d", 5),
                Map.of("name", "item2", "x", Map.of("d", 5)),
                Map.of("name", "item3", "x", Map.of("y", Map.of("d", 5))),
                Map.of("name", "item4", "other", "value"),
                Map.of("name", "item5", "d", 10),
                Map.of("name", "item6", "x", Map.of("d", 10))
            )
        );
        // Use 'in' operator with recursive descent - much cleaner!
        List<Map<String, Object>> results = JsonPath.read(data, "$.items[?(5 in @..d)]");
        System.out.println("  → Non-homogeneous with 'in @..d': " + results.size() + " items");
        if (results.size() != 3) throw new AssertionError("Expected 3");
    }

    static void testSelectParentCollection_IfChildrenMatch() {
        Map<String, Object> data = Map.of(
            "collections", List.of(
                Map.of("name", "collection1", "items", List.of(
                    Map.of("status", "active"),
                    Map.of("status", "inactive")
                )),
                Map.of("name", "collection2", "items", List.of(
                    Map.of("status", "inactive"),
                    Map.of("status", "inactive")
                )),
                Map.of("name", "collection3", "items", List.of(
                    Map.of("status", "active"),
                    Map.of("status", "active")
                ))
            )
        );
        // Using 'in' operator to check if 'active' exists in the items[*].status array
        List<List<Map<String, Object>>> results = JsonPath.read(data,
            "$.collections[?('active' in @.items[*].status)].items");
        System.out.println("  → Parent collections with active children: " + results.size());
        if (results.size() != 2) throw new AssertionError("Expected 2 collections");
    }

    static void testSelectParentCollection_GetPaths() {
        Map<String, Object> data = Map.of(
            "collections", List.of(
                Map.of("name", "collection1", "items", List.of(Map.of("status", "active"))),
                Map.of("name", "collection2", "items", List.of(Map.of("status", "inactive"))),
                Map.of("name", "collection3", "items", List.of(Map.of("status", "active")))
            )
        );
        Configuration conf = Configuration.builder().options(Option.AS_PATH_LIST).build();
        List<String> paths = JsonPath.using(conf).parse(data).read(
            "$.collections[?('active' in @.items[*].status)].items");
        System.out.println("  → Paths to parent collections: " + paths);
        if (paths.size() != 2) throw new AssertionError("Expected 2 paths");
    }

    static void testArrayContains_InOperator() {
        Map<String, Object> data = Map.of(
            "items", List.of(
                Map.of("name", "item1", "tags", List.of("red", "blue")),
                Map.of("name", "item2", "tags", List.of("green", "target")),
                Map.of("name", "item3", "tags", List.of("yellow"))
            )
        );
        List<Map<String, Object>> results = JsonPath.read(data, "$.items[?('target' in @.tags)]");
        System.out.println("  → 'in' operator works! Found: " + results.size() + " item(s)");
        if (results.size() != 1) throw new AssertionError("Expected 1");
        if (!"item2".equals(results.get(0).get("name"))) throw new AssertionError("Expected item2");
    }

    static void testDescendantFilter_FindObjectsAtAnyDepth() {
        Map<String, Object> data = Map.of(
            "items", List.of(
                Map.of("id", 1, "d", 5, "x", Map.of("e", 10)),
                Map.of("id", 2, "e", 10, "y", Map.of("d", 5)),
                Map.of("id", 3, "d", 5, "other", "value"),
                Map.of("id", 4, "e", 10, "other", "value"),
                Map.of("id", 5, "a", Map.of("b", Map.of("d", 5, "e", 10)))
            )
        );
        // $..[?(@.d == 5)] finds all objects at ANY depth where d == 5 at that object's level
        List<Map<String, Object>> results = JsonPath.read(data, "$..[?(@.d == 5)]");
        System.out.println("  → $..[?(@.d == 5)] finds objects at any depth: " + results.size() + " objects");
        if (results.size() != 4) throw new AssertionError("Expected 4 objects with d==5");
    }

    static void testReturnSingleCollection_IfNestedConditionMatches() {
        // Case 1: Collection with items that have d==5 (should return the collection)
        Map<String, Object> data1 = Map.of(
            "collection", Map.of(
                "name", "myCollection",
                "metadata", Map.of("owner", "Alice"),
                "items", List.of(
                    Map.of("name", "item1", "d", 5),
                    Map.of("name", "item2", "x", Map.of("d", 10))
                )
            )
        );
        // $..[?(5 in @.items..d)] finds objects at any depth that have items with d==5
        List<Map<String, Object>> results = JsonPath.read(data1, "$..[?(5 in @.items..d)]");
        System.out.println("  → Return collection object: " + results.size() + " collection (name: " + results.get(0).get("name") + ")");
        if (results.size() != 1) throw new AssertionError("Expected 1 collection");
        if (!"myCollection".equals(results.get(0).get("name"))) throw new AssertionError("Expected myCollection");

        // Case 2: Collection WITHOUT items that have d==5 (should return empty)
        Map<String, Object> data2 = Map.of(
            "collection", Map.of(
                "name", "emptyCollection",
                "items", List.of(
                    Map.of("name", "item3", "d", 10),
                    Map.of("name", "item4", "x", Map.of("d", 20))
                )
            )
        );
        List<Map<String, Object>> results2 = JsonPath.read(data2, "$..[?(5 in @.items..d)]");
        if (results2.size() != 0) throw new AssertionError("Expected 0 collections");
    }
}

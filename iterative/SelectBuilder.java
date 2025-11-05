import generated.*;
import org.antlr.v4.runtime.*;
import java.util.*;

public class SelectBuilder {
	private String selectQuery;
	private Object source; // Value or ValueVector
	private String whereQuery = "";
	private String rootPath = "";

	public SelectBuilder select(String query) {
		this.selectQuery = query;
		return this;
	}

	public SelectBuilder from(Value tree, String path) {
		this.source = tree;
		this.rootPath = path;
		return this;
	}

	public SelectBuilder from(ValueVector array, String path) {
		this.source = array;
		this.rootPath = path;
		return this;
	}

	public SelectBuilder where(String query) {
		this.whereQuery = query;
		return this;
	}

	public List<String> exec() {
		Objects.requireNonNull(source);
		Objects.requireNonNull(selectQuery);
		SelectQueryParser parser = new SelectQueryParser(new CommonTokenStream(
			new SelectQueryLexer(CharStreams.fromString(selectQuery))));
		return new QueryExecutor(source, whereQuery, rootPath).execute(parser.selectClause());
	}

	record PathNode(Value node, String path, int segmentIndex) {}

	private static class QueryExecutor {
		private Object source;
		private String rootPath;
		private List<String> results = new ArrayList<>();
		private List<SelectQueryParser.SegmentContext> segments;
		private WhereEvaluator whereEval;

		QueryExecutor(Object source, String whereQuery, String rootPath) {
			this.source = source;
			this.rootPath = rootPath;

			// Parse WHERE once if provided
			if (!whereQuery.trim().isEmpty()) {
				SelectQueryParser parser = new SelectQueryParser(new CommonTokenStream(
					new SelectQueryLexer(CharStreams.fromString(whereQuery.trim()))));
				whereEval = new WhereEvaluator(parser.whereClause());
			}
		}

		List<String> execute(SelectQueryParser.SelectClauseContext ctx) {
			segments = ctx.segment();

			// Start navigation
			Deque<PathNode> stack = new ArrayDeque<>();

			switch (source) {
				case ValueVector arr -> pushArray(stack, arr, rootPath, 0);
				case Value tree -> stack.push(new PathNode(tree, rootPath, 0));
				default -> throw new IllegalStateException();
			}

			while (!stack.isEmpty()) {
				PathNode current = stack.pop();

				// Terminal: test and collect
				if (current.segmentIndex >= segments.size()) {
					if (whereEval == null || whereEval.test(current.node)) {
						results.add(current.path);
					}
					continue;
				}

				// Process segment
				switch (segments.get(current.segmentIndex)) {
					case SelectQueryParser.DescendantArraySegmentContext seg ->
						descendant(current, seg.ID().getText(), true, stack);
					case SelectQueryParser.DescendantSegmentContext seg ->
						descendant(current, seg.ID().getText(), false, stack);
					case SelectQueryParser.DotSegmentContext seg ->
						dot(current, seg.token(), stack);
					default -> throw new IllegalStateException();
				}
			}

			return results;
		}

		// Example: $.*, $.*[*], $.field, $.field[*]
		private void dot(PathNode current, SelectQueryParser.TokenContext token, Deque<PathNode> stack) {
			int next = current.segmentIndex + 1;

			switch (token) {
				case SelectQueryParser.WildcardArrayContext t ->
					current.node.children().forEach((n, a) -> pushArray(stack, a, path(current.path, n), next));
				case SelectQueryParser.WildcardContext t ->
					current.node.children().forEach((n, a) -> pushFirst(stack, a, path(current.path, n), next));
				case SelectQueryParser.FieldArrayContext t -> {
					String name = t.ID().getText();
					if (current.node.hasChildren(name))
						pushArray(stack, current.node.getChildren(name), path(current.path, name), next);
				}
				case SelectQueryParser.FieldContext t -> {
					String name = t.ID().getText();
					if (current.node.hasChildren(name))
						pushFirst(stack, current.node.getChildren(name), path(current.path, name), next);
				}
				default -> throw new IllegalStateException();
			}
		}

		// Example: $..name, $..tags[*]
		private void descendant(PathNode current, String fieldName, boolean allElements, Deque<PathNode> stack) {
			int next = current.segmentIndex + 1;

			// Check current node
			if (current.node.hasChildren(fieldName)) {
				ValueVector arr = current.node.getChildren(fieldName);
				String p = path(current.path, fieldName);

				if (allElements) {
					pushArray(stack, arr, p, next);
				} else {
					pushFirst(stack, arr, p, next);
				}
			}

			// Continue searching in children - explore ALL array elements for descendant search
			current.node.children().forEach((n, a) -> {
				if (a.size() == 1) {
					// Single element - use short form (a.b = a.b[0])
					stack.push(new PathNode(a.first(), path(current.path, n), current.segmentIndex));
				} else {
					// Multiple elements - use indexed form
					for (int i = 0; i < a.size(); i++) {
						stack.push(new PathNode(a.get(i), path(current.path, n) + "[" + i + "]", current.segmentIndex));
					}
				}
			});
		}

		private void pushArray(Deque<PathNode> stack, ValueVector arr, String basePath, int next) {
			for (int i = 0; i < arr.size(); i++)
				stack.push(new PathNode(arr.get(i), basePath + "[" + i + "]", next));
		}

		private void pushFirst(Deque<PathNode> stack, ValueVector arr, String p, int next) {
			if (!arr.isEmpty()) stack.push(new PathNode(arr.first(), p, next));
		}

		private String path(String curr, String child) {
			return curr.isEmpty() ? child : curr + "." + child;
		}
	}

	// ========== WHERE EVALUATION (RECURSIVE) ==========

	private static class WhereEvaluator {
		private SelectQueryParser.WhereClauseContext whereTree;

		WhereEvaluator(SelectQueryParser.WhereClauseContext whereTree) {
			this.whereTree = whereTree;
		}

		boolean test(Value node) {
			// OR: at least one clause must be true
			return whereTree.orExpr().andExpr().stream().anyMatch(c -> evalAnd(node, c));
		}

		// AND: all clauses must be true
		private boolean evalAnd(Value node, SelectQueryParser.AndExprContext ctx) {
			return ctx.notExpr().stream().allMatch(c -> evalNot(node, c));
		}

		// NOT: negate expression
		private boolean evalNot(Value node, SelectQueryParser.NotExprContext ctx) {
			return switch (ctx) {
				case SelectQueryParser.NotExpressionContext c -> !evalNot(node, c.notExpr());
				case SelectQueryParser.PrimaryExpressionContext c -> evalPrimary(node, c.primary());
				default -> throw new IllegalStateException();
			};
		}

		// Parentheses or condition
		private boolean evalPrimary(Value node, SelectQueryParser.PrimaryContext ctx) {
			return switch (ctx) {
				case SelectQueryParser.ParenExpressionContext c ->
					c.orExpr().andExpr().stream().anyMatch(a -> evalAnd(node, a));
				case SelectQueryParser.ConditionExpressionContext c -> evalCond(node, c.condition());
				default -> throw new IllegalStateException();
			};
		}

		// Leaf condition
		private boolean evalCond(Value node, SelectQueryParser.ConditionContext ctx) {
			return switch (ctx) {
				case SelectQueryParser.NodeValueContext c ->
					eq(node, c.value().getText());
				case SelectQueryParser.PathExistsContext c ->
					evalPathExists(node, c.wherePath());
				case SelectQueryParser.PathMatchContext c ->
					evalPath(node, c.wherePath(), c.value().getText());
				default -> throw new IllegalStateException();
			};
		}

		// Check if path exists (e.g., .b.c in .)
		private boolean evalPathExists(Value node, SelectQueryParser.WherePathContext pathCtx) {
			return walkDirectPath(node, pathCtx.wherePathSegment()) != null;
		}

		// Walk a direct path and return final node, or null if path doesn't exist
		private Value walkDirectPath(Value node, List<SelectQueryParser.WherePathSegmentContext> segs) {
			if (segs.isEmpty()) return null;

			Value current = node;
			// Walk the path: .field1.field2.field3...
			for (SelectQueryParser.WherePathSegmentContext seg : segs) {
				if (seg instanceof SelectQueryParser.WhereDirectSegmentContext direct) {
					String fieldName = direct.ID().getText();
					// Anti-vivification: check existence before accessing
					if (!current.hasChildren(fieldName)) return null;
					ValueVector arr = current.getChildren(fieldName);
					if (arr.isEmpty()) return null;
					current = arr.first(); // a.b = a.b[0]
				} else {
					// Descendant patterns not supported
					return null;
				}
			}
			return current;
		}

		/**
		 * Evaluates WHERE path conditions: .field = value, ..field = value, ..field[*] = value
		 *
		 * Flow:
		 * 1. Get path segments from parse tree
		 * 2. Check first segment type using switch:
		 *    - ..field[*] → descendant array search (check all elements)
		 *    - ..field    → descendant search (check first element only, a.b = a.b[0])
		 *    - .field     → direct path navigation
		 * 3. For descendant patterns:
		 *    - Recursively search all descendants via searchDesc()
		 * 4. For direct patterns:
		 *    - Walk path via walkDirectPath() and check if final node's value matches
		 */
		private boolean evalPath(Value node, SelectQueryParser.WherePathContext pathCtx, String expected) {
			List<SelectQueryParser.WherePathSegmentContext> segs = pathCtx.wherePathSegment();
			if (segs.isEmpty()) return false;

			SelectQueryParser.WherePathSegmentContext first = segs.get(0);

			return switch (first) {
				// Example: WHERE ..tags[*] = admin (ctx.ID() is fieldName)
				case SelectQueryParser.WhereDescendantArraySegmentContext ctx ->
					searchDesc(node, ctx.ID().getText(), expected, true);
				// Example: WHERE ..status = active (ctx.ID() is fieldName)
				case SelectQueryParser.WhereDescendantSegmentContext ctx ->
					searchDesc(node, ctx.ID().getText(), expected, false);
				// Example: WHERE .user.name = Alice
				case SelectQueryParser.WhereDirectSegmentContext ctx ->
					walkDirectPath(node, segs) instanceof Value v && eq(v, expected);
				default -> throw new IllegalStateException();
			};
		}

		/**
		 * Recursively searches descendants for a field with matching value
		 *
		 * Flow:
		 * 1. Check if current node has the target field
		 *    - If allElements=true: check all array elements (..field[*])
		 *    - If allElements=false: check first element only (..field, a.b = a.b[0])
		 * 2. If match found, return true immediately
		 * 3. Otherwise, recursively search all children:
		 *    a. Iterate through all child fields
		 *    b. For each field, search its first element only (a.b = a.b[0])
		 *    c. Continue until match found or all descendants explored
		 *
		 * Example: node.items[0].tags[*] where items has tags field
		 * - Check node.items (if allElements, check all; else first only)
		 * - Recurse into node.items[0] (first element)
		 * - Check node.items[0].tags (if allElements, check all; else first only)
		 */
		private boolean searchDesc(Value node, String fieldName, String expected, boolean allElements) {
			// Check if current node has the target field
			if (node.hasChildren(fieldName)) {
				ValueVector arr = node.getChildren(fieldName);

				if (allElements) {
					// Check all array elements: ..field[*] = value
					for (int i = 0; i < arr.size(); i++)
						if (eq(arr.get(i), expected)) return true;
				} else {
					// Check first element only: ..field = value (a.b = a.b[0])
					if (!arr.isEmpty() && eq(arr.first(), expected)) return true;
				}
			}

			// Recursively search all children (a.b = a.b[0])
			for (ValueVector arr : node.children().values()) {
				if (!arr.isEmpty() && searchDesc(arr.first(), fieldName, expected, allElements)) return true;
			}

			return false;
		}

		private boolean eq(Value v, String exp) {
			try {
				return (v.isInt() && v.intValue() == Integer.parseInt(exp)) ||
					   (v.isString() && v.strValue().equals(exp)) ||
					   (v.isBool() && v.boolValue() == Boolean.parseBoolean(exp));
			} catch (NumberFormatException e) {
				return false;
			}
		}
	}
}

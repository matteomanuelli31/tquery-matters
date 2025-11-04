# SELECT Query Builder for Jolie Trees

## Core Principle

**Everything is an array in Jolie:**
```
a.b = a.b[0]    // Single value is array element [0]
a.b[*]          // Explicit: all array elements
```

This principle is consistently applied throughout the SELECT implementation.

## Pattern Syntax

### Field Navigation

| Pattern | Description | Example | Result |
|---------|-------------|---------|--------|
| `field` | Navigate to `field[0]` (first element) | `SELECT a.b.c` | Navigate to `a[0].b[0].c[0]` |
| `field[*]` | All elements in field's array | `SELECT a.users[*]` | All `a.users[i]` |

### Wildcards

| Pattern | Description | Example | Result |
|---------|-------------|---------|--------|
| `*` | All field names (checks `[0]` only) | `SELECT a.*` | `["a.users", "a.posts"]` |
| `*[*]` | All fields, all array elements | `SELECT a.*[*]` | `["a.users[0]", "a.users[1]", "a.posts[0]"]` |

### Descendant Search

| Pattern | Description | Example | Result |
|---------|-------------|---------|--------|
| `..field` | Find field at any depth (checks `[0]` only) | `SELECT ..name` | All field paths named "name" |
| `..field[*]` | Find field at any depth, all elements | `SELECT ..tags[*]` | All array elements in any "tags" field |

## WHERE Conditions

| Syntax | Description | Example |
|--------|-------------|---------|
| `. = value` | Node's own value equals | `WHERE . = 5` |
| `.field = value` | Direct child equals (1 level) | `WHERE .name = Alice` |
| `..field = value` | Descendant equals (any depth) | `WHERE ..age = 30` |
| `field in .` | Has child field (existence) | `WHERE schema in .` |

**Supported types:** Integer, String, Boolean

## API Usage

```java
List<String> results = new SelectBuilder()
    .select("pattern")
    .from(valueTree)
    .where("condition")  // optional
    .exec();
```

## Implementation Details

### Anti-Vivification
- Uses `hasChildren()` checks before `getChildren()` to avoid creating fields during read-only queries
- Uses `isEmpty()` checks before `.first()` to avoid auto-expansion

### Generalization
- `a.b = a.b[0]` principle: field patterns without `[*]` check only first element
- `field[*]` explicitly requests all array elements
- No serialization/deserialization overhead

## Detailed Test Descriptions

### 1. SelectTest - Descendant Search with Wildcard

**Input Structure:**
```
a.b.c.d.e = 5
a.b.e = 5
```

**Query:** `SELECT $.b.* FROM a WHERE ..e = 5`

**Output:** `[a.b.e, a.b.c]`

---

### 2. SelectArrayTest - Array Filtering with WHERE Clauses

**Input Structure:**
```
apiData.operations[0]: { path: "/users", method: "get", tags: ["users", "admin"], requiresAuth: true }
apiData.operations[1]: { path: "/public/posts", method: "get", tags: ["posts", "public"], requiresAuth: false }
apiData.operations[2]: { path: "/admin/secrets", method: "post", tags: ["secrets", "sensitive"], requiresAuth: true }
apiData.operations[3]: { path: "/admin/logs", method: "get", tags: ["admin", "monitoring"], requiresAuth: true }
```

**Test 1:** `SELECT $.operations[*] FROM apiData WHERE .method = get`
- **Output:** `[apiData.operations[0], apiData.operations[1], apiData.operations[3]]`

**Test 2:** `SELECT $.operations[*] FROM apiData WHERE .requiresAuth = true`
- **Output:** `[apiData.operations[0], apiData.operations[2], apiData.operations[3]]`

**Test 3:** `SELECT $.operations[*] FROM apiData WHERE ..tags = admin`
- **Output:** `[apiData.operations[3]]`
- **Note:** Only checks `tags[0]` (following `a.b = a.b[0]` principle)

**Test 4:** `SELECT $.operations[*] FROM apiData WHERE ..tags[*] = admin`
- **Output:** `[apiData.operations[0], apiData.operations[3]]`
- **Note:** Explicitly checks all array elements with `[*]` syntax

---

### 3. SelectWildcardTest - Wildcard * vs *[*]

**Input Structure:**
```
a.b[0] = "ciao"
a.b[1] = "hello"
a.c[0] = "ciao"
```

**Test 1:** `SELECT $.* FROM a WHERE . = ciao`
- **Output:** `[a.b, a.c]`
- **Note:** Returns field names where `field[0] = "ciao"`

**Test 2:** `SELECT $.*[*] FROM a WHERE . = ciao`
- **Output:** `[a.b[0], a.c[0]]`
- **Note:** Returns specific array indices that equal "ciao"

**Test 3:** `SELECT $.*[*] FROM a WHERE . = hello`
- **Output:** `[a.b[1]]`
- **Note:** Only `a.b[1]` has value "hello"

---

### 4. SelectParameterFilteringTest - Heterogeneous Structures

**Input Structure:**
```
data.paths[0].methods[0].parameters[0]: { in: "query", name: "page", type: "integer" }
data.paths[0].methods[0].parameters[1]: { in: "header", name: "Authorization", type: "string" }
data.paths[0].methods[1].parameters[0]: { in: "body", name: "user", schema: "UserSchema" }  // NO TYPE!
data.paths[0].methods[1].parameters[1]: { in: "header", name: "Content-Type", type: "string" }
data.paths[1].methods[0].parameters[0]: { in: "path", name: "id", type: "string" }
data.paths[1].methods[0].parameters[1]: { in: "header", name: "Authorization", type: "string" }
```

**Test 1:** `SELECT $.paths[*].methods[*].parameters[*] WHERE .in = body`
- **Output:** `[data.paths[0].methods[1].parameters[0]]`

**Test 2:** `SELECT $.paths[*].methods[*].parameters[*] WHERE .in = path`
- **Output:** `[data.paths[1].methods[0].parameters[0]]`

**Test 3:** `SELECT $.paths[*].methods[*].parameters[*] WHERE .in = query`
- **Output:** `[data.paths[0].methods[0].parameters[0]]`

**Test 4:** `SELECT $.paths[*].methods[*].parameters[*] WHERE .in = header`
- **Output:** `[data.paths[0].methods[0].parameters[1], data.paths[0].methods[1].parameters[1], data.paths[1].methods[0].parameters[1]]`

**Test 5:** `SELECT $.paths[*].methods[*].parameters[*] WHERE schema in .`
- **Output:** `[data.paths[0].methods[1].parameters[0]]`
- **Note:** Only body param has `schema` field instead of `type`

**Test 6:** `SELECT $.paths[*].methods[*].parameters[*] WHERE type in .`
- **Output:** `[data.paths[0].methods[0].parameters[0], data.paths[0].methods[0].parameters[1], data.paths[0].methods[1].parameters[1], data.paths[1].methods[0].parameters[0], data.paths[1].methods[0].parameters[1]]`
- **Note:** All except body param have `type` field (5 results)

---

### 5. SelectRelativePathTest - Simple Path Navigation

**Input Structure:**
```
a.b.c = 5
```

**Query:** `SELECT $.b.c FROM a WHERE . = 5`

**Output:** `[a.b.c]`

---

### 6. SelectDescendantTest - Descendant Search

**Input Structure:**
```
root.x.value = 10
root.y.z.value = 10
```

**Query:** `SELECT $..value FROM root WHERE . = 10`

**Output:** `[root.x.value, root.y.z.value]`

---

### 7. SelectDescendantArrayTest - Descendant Array Search

**Input Structure:**
```
root.items[0].price = 100
root.items[1].price = 200
```

**Query:** `SELECT $..items[*] FROM root WHERE .price = 100`

**Output:** `[root.items[0]]`

---

### 9. SelectSimplePathTest - Simple Path Selections

**Input Structure:**
```
a.b.c = 5
a.b.d = 10
a.e = 5
```

**Test 1:** `SELECT $.b.c FROM a WHERE . = 5`
- **Output:** `[a.b.c]`

**Test 2:** `SELECT $.b.d FROM a WHERE . = 5`
- **Output:** `[]` (a.b.d = 10, not 5)

**Test 3:** `SELECT $.e FROM a WHERE . = 5`
- **Output:** `[a.e]`

## Design Philosophy

+ **Generalization** - Consistent `a.b = a.b[0]` semantics
+ **Safety** - No side effects during queries (anti-vivification)

## Future Extensions (Not Implemented)

- Arithmetic comparisons (`>`, `<`, `>=`, `<=`)
- Logical operators (`AND`, `OR`, `NOT`)
- Regular expression matching
- Projection (selecting specific subfields)
- Aggregation (count, sum, etc.)

---

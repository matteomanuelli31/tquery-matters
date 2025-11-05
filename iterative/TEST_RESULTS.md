# SELECT Query Test Results

## Test Suite 1: SelectSimpleTest

### Simple Path Navigation with WHERE

**Input Structure:**
```
a
└─ b
   └─ c = 5
```

**Query:**
```java
SELECT $.b.c FROM a WHERE . = 5
```

**Output:**
```
a.b.c
```

---

### Wildcard Selection with WHERE

**Input Structure:**
```
root
├─ x = 10
├─ y = 20
└─ z = 10
```

**Query:**
```java
SELECT $.* FROM root WHERE . = 10
```

**Output:**
```
root.x
root.z
```

---

### Descendant Search

**Input Structure:**
```
tree
├─ a
│  └─ value = 42
└─ b
   └─ c
      └─ value = 42
```

**Query:**
```java
SELECT $..value FROM tree WHERE . = 42
```

**Output:**
```
tree.a.value
tree.b.c.value
```

---

## Test Suite 2: SelectArrayTest

### AND Operator

**Input Structure:**
```
items (ValueVector)
├─ [0]: type = user, status = active
├─ [1]: type = admin, status = active
├─ [2]: type = user, status = inactive
└─ [3]: type = user, status = active
```

**Query:**
```java
SELECT $ FROM items WHERE .status = active && .type = user
```

**Output:**
```
items[0]
items[3]
```

---

### OR Operator

**Input Structure:**
```
items (ValueVector)
├─ [0]: status = active
├─ [1]: status = pending
├─ [2]: status = inactive
└─ [3]: status = completed
```

**Query:**
```java
SELECT $ FROM items WHERE .status = active || .status = completed
```

**Output:**
```
items[0]
items[3]
```

---

### NOT Operator

**Input Structure:**
```
items (ValueVector)
├─ [0]: type = admin, status = active
├─ [1]: type = user, status = active
└─ [2]: type = admin, status = inactive
```

**Query:**
```java
SELECT $ FROM items WHERE .status = active && !.type = admin
```

**Output:**
```
items[1]
```

---

### Parentheses for Precedence

**Input Structure:**
```
items (ValueVector)
├─ [0]: type = premium, status = active, verified = true
├─ [1]: type = free, status = active, verified = true
├─ [2]: type = premium, status = inactive, verified = true
└─ [3]: type = premium, status = active, verified = false
```

**Query:**
```java
SELECT $ FROM items WHERE (.type = premium || .status = active) && .verified = true
```

**Output:**
```
items[0]
items[1]
```

---

## Test Suite 3: SelectNestedTest

### Nested Navigation with Child Field Filter

**Input Structure:**
```
data (ValueVector)
├─ [0]
│  └─ a
│     └─ b = 6
└─ [1]
   └─ a
      └─ b = 5
```

**Query:**
```java
SELECT $.a FROM data WHERE .b = 5
```

**Output:**
```
data[1].a
```

---

## Test Suite 4: SelectFieldTest

### Field Existence AND Value Check

**Input Structure:**
```
items (ValueVector)
├─ [0]: name="Alice", age=30, email="alice@example.com"
├─ [1]: name="Bob", age=25, email="bob@example.com"
├─ [2]: name="Charlie", age=30
└─ [3]: name="David", email="david@example.com"
```

**Query:**
```java
SELECT $ FROM items WHERE .email in . && .age = 30
```

**Output:**
```
items[0]
```

---

### Path Traversal in WHERE

**Input Structure:**
```
data (ValueVector)
├─ [0]: a{id=1, b{c=6}}
├─ [1]: a{id=2, b{c=5}}
├─ [2]: a{id=3}
└─ [3]: a{id=4, b{x=10}}
```

**Query:**
```java
SELECT $.a FROM data WHERE .b.c = 5
```

**Output:**
```
data[1].a
```

---

### Nested Path Existence

**Input Structure:**
```
users (ValueVector)
├─ [0]: name="Alice", settings{notifications{enabled=true}}
├─ [1]: name="Bob", settings{theme="dark"}
└─ [2]: name="Charlie"
```

**Query:**
```java
SELECT $ FROM users WHERE .settings.notifications in .
```

**Output:**
```
users[0]
```

---

### Direct Field with Single Dot

**Input Structure:**
```
projects (ValueVector)
├─ [0]: status="active", technologies="Python"
├─ [1]: status="active", technologies="Java"
├─ [2]: status="inactive", technologies="Python"
└─ [3]: status="active"
```

**Query:**
```java
SELECT $ FROM projects WHERE .status = active && .technologies = Python
```

**Output:**
```
projects[0]
```

---

### Multi-Level Path Traversal

**Input Structure:**
```
accounts (ValueVector)
├─ [0]: user{settings{theme="dark"}}
├─ [1]: user{settings{theme="light"}}
└─ [2]: user{settings{language="en"}}
```

**Query:**
```java
SELECT $ FROM accounts WHERE .user.settings.theme = dark
```

**Output:**
```
accounts[0]
```

---

## Test Suite 5: SelectCompaniesTest

**Note:** For readability, outputs show project IDs extracted from paths. The actual SELECT output returns full paths like `companies[0].company.departments[0].teams[0].projects[0]`.

### Companies.json Structure

```
companies.json:
  companies[0..10]
    └─ company
         ├─ name
         ├─ founded
         ├─ headquarters
         │    ├─ city
         │    ├─ country
         │    └─ address
         └─ departments[0..N]
              ├─ id
              ├─ name
              ├─ manager
              └─ teams[0..N]
                   ├─ team_id
                   ├─ team_name
                   └─ projects[0..N]
                        ├─ project_id
                        ├─ name
                        ├─ status (in_progress, planning, completed, testing)
                        └─ technologies[]
```

**Statistics:**
- Total companies: 11
- Total projects: 33
- Status breakdown: completed: 7, in_progress: 10, planning: 11, testing: 5

---

### AND Operator - in_progress AND Python

**Query:**
```java
SELECT $..projects[*] FROM companies
WHERE .status = in_progress && ..technologies[*] = Python
```

**Output (Project IDs):**
```
P001, P202, P600, P801
```

---

### OR Operator - in_progress OR completed

**Query:**
```java
SELECT $..projects[*] FROM companies
WHERE .status = in_progress || .status = completed
```

**Output (Project IDs):**
```
P001, P004, P100, P101, P201, P202, P300, P500, P501,
P600, P702, P800, P801, P900, P901, P1000, P1001
```

**Count:** 17 projects

---

### NOT Operator - NOT completed

**Query:**
```java
SELECT $..projects[*] FROM companies
WHERE !.status = completed
```

**Output (Project IDs):**
```
P001, P002, P003, P101, P102, P103, P200, P202, P300, P301, P302,
P400, P401, P500, P502, P600, P601, P602, P700, P701, P702,
P801, P802, P900, P1001, P1002
```

**Count:** 26 projects

---

### Complex Boolean Expression with Parentheses

**Query:**
```java
SELECT $..projects[*] FROM companies
WHERE (.status = in_progress && ..technologies[*] = Python)
   || (.status = completed && ..technologies[*] = Java)
```

**Output (Project IDs):**
```
P001, P202, P600, P801
```

---

## Summary

### Key Features Demonstrated

1. **Path Navigation**
   - Direct paths: `$.b.c`
   - Wildcards: `$.*`, `$.*[*]`
   - Field arrays: `$.field[*]`
   - Descendant search: `$..value`, `$..projects[*]`

2. **WHERE Clause Evaluation**
   - Node value: `. = 5`
   - Field existence: `.field in .`
   - Direct field value: `.status = active`
   - Nested path existence: `.a.b.c in .`
   - Nested path value: `.a.b.c = value`
   - Descendant field (first element): `..technologies = Python`
   - Descendant field (all elements): `..technologies[*] = Python`
   - Boolean operators: `&&`, `||`, `!`
   - Parentheses for precedence: `(A || B) && C`

3. **Data Types**
   - Value tree input: `from(Value, "path")`
   - Array input: `from(ValueVector, "path")`

4. **Real-World Complexity**
   - Nested arrays at multiple levels (companies → departments → teams → projects)
   - Complex boolean queries with multiple conditions
   - Descendant search across deeply nested structures

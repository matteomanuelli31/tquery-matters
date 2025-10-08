# get tquery

## prerequisites
+ `java` jdk 21
+ `jolie` installed
+ `npm`

```bash
git clone https://github.com/jolie/tquery.git
cd tquery
# tquery's repo ships with gradle 7.0 wrapper
# substitute in place to support Java 21
sed -i 's/gradle-7.0-bin.zip/gradle-8.5-bin.zip/' gradle/wrapper/gradle-wrapper.properties
# build tquery (will create build/libs/tquery-0.4.10.jar w/ java classes)
./gradlew build
# jolie looks for java libs in `lib/`
cd ..
mkdir -p lib
cp tquery/build/libs/tquery-0.4.10.jar lib/

npm init -y
# install tquery package from npm (for jolie module definitions)
npm install @jolie/tquery --save

# create packages directory structure
mkdir -p packages/@jolie

# copy tquery module files to packages directory
cp -r node_modules/@jolie/tquery packages/@jolie/


# create test file
cat > test_tquery.ol << 'EOF'
from @jolie.tquery.main import TQuery
from console import Console

service Test {
    embed TQuery as TQuery
    embed Console as Console

    main {
        testData.records[0].name = "Alice";
        testData.records[1].name = "Bob";

        unwind@TQuery({
            data << testData
            query = "records"
        })(result);

        println@Console("unwind result count: " + #result.result)()
    }
}
EOF

# run test (should output: "unwind result count: 2")
jolie test_tquery.ol

rm test_tquery.ol
```

# use cases where tquery would save our bacon

## **1) Problem:** filter all operations s.t.: method == "get" AND requiresAuth == true AND "admin" in tags. simple - isn't it?
(see working example [here](nasty-nested.ol))

```jolie
apiData.operations[0] << {
    path = "/users"
    method = "get"
    tags[0] = "users"
    tags[1] = "admin"
    requiresAuth = true
};
apiData.operations[1] << {
    path = "/public/posts"
    method = "get"
    tags[0] = "posts"
    tags[1] = "public"
    requiresAuth = false
};
apiData.operations[2] << {
    path = "/admin/secrets"
    method = "post"
    tags[0] = "admin"
    tags[1] = "sensitive"
    requiresAuth = true
};
apiData.operations[3] << {
    path = "/admin/logs"
    method = "get"
    tags[0] = "admin"
    tags[1] = "monitoring"
    requiresAuth = true
};

// ===================================================================
// WITHOUT TQUERY: nasty nested blocks - no declarative as Jolie...
// ===================================================================
for (i = 0, i < #apiData.operations, i++) {
    if (apiData.operations[i].method == "get") {
        if (apiData.operations[i].requiresAuth == true) {
            hasAdminTag = false;
            for (j = 0, j < #apiData.operations[i].tags, j++) {
                if (apiData.operations[i].tags[j] == "admin") {
                    hasAdminTag = true;
                    break
                }
            };
            if (hasAdminTag) {
                result[#result] << apiData.operations[i]
            }
        }
    }
}

// ===================================================================
// WITH TQUERY
// ===================================================================

unwind@TQuery({
    data << apiData
    query = "operations"
})(unwoundOps);

/* TRANSFORMATION:
   INPUT: apiData = { operations: [ {op0}, {op1}, {op2}, {op3} ] }

   OUTPUT (unwoundOps.result): [
     { operations: {path="/users", method="get", tags=[...], requiresAuth=true} },      ← Record 0
     { operations: {path="/public/posts", method="get", tags=[...], requiresAuth=false} }, ← Record 1
     { operations: {path="/admin/secrets", method="post", tags=[...], requiresAuth=true} }, ← Record 2
     { operations: {path="/admin/logs", method="get", tags=[...], requiresAuth=true} }   ← Record 3
   ]
*/

// filter
match@TQuery({
    data << unwoundOps.result
    query.and << {
        left.equal << { path = "operations.method" data = "get" }
        right.equal << { path = "operations.requiresAuth" data = true }
    }
})(filteredOps);

/* TRANSFORMATION:
   INPUT: 4 records (all operations)

   OUTPUT (filteredOps.result): [
     { operations: {path="/users", method="get", tags=["users","admin"], requiresAuth=true} },
     { operations: {path="/admin/logs", method="get", tags=["admin","monitoring"], requiresAuth=true} }
   ]
*/

// unwind "tags" array
unwind@TQuery({
    data << filteredOps.result
    query = "operations.tags"
})(unwoundTags);

/* TRANSFORMATION:
   INPUT: [
     { operations: {path="/users", method="get", tags=["users","admin"], requiresAuth=true} },
     { operations: {path="/admin/logs", method="get", tags=["admin","monitoring"], requiresAuth=true} }
   ]

   OUTPUT (unwoundTags.result): [
     { operations: {path="/users", method="get", tags="users", requiresAuth=true} },     ← Tag 0
     { operations: {path="/users", method="get", tags="admin", requiresAuth=true} },     ← Tag 1
     { operations: {path="/admin/logs", method="get", tags="admin", requiresAuth=true} }, ← Tag 0
     { operations: {path="/admin/logs", method="get", tags="monitoring", requiresAuth=true} } ← Tag 1
   ]
*/

// filter
match@TQuery({
    data << unwoundTags.result
    query.equal << { path = "operations.tags" data = "admin" }
})(result);

/* TRANSFORMATION:
   INPUT: 4 tag records (2 operations × ~2 tags each)

   OUTPUT (result.result): [
     { operations: {path="/users", method="get", tags="admin", requiresAuth=true} },
     { operations: {path="/admin/logs", method="get", tags="admin", requiresAuth=true} }
   ]
*/

```

**Key Insight:** The nested structure:
```
for operations          ← LOOP 1
  if method == get      ← CHECK 1
    if requiresAuth     ← CHECK 2
      for tags          ← LOOP 2 (nested inside!)
        if tag == admin ← CHECK 3
          if hasAdminTag ← CHECK 4
            ADD RESULT  ← 4 levels deep!
```

Becomes a **pure declarative pipeline** (no loops, no conditionals):
```
unwind "operations" → match (method AND auth) → unwind "operations.tags" → match (admin)
```


## **1b)**: Problem: Find All "in_progress" Projects Across All Companies from [companies.json](companies.json) (simple json analysis)
showing how tquery's nested path unwinding eliminates 4 levels of nested loops

(to run execute from top root dir `jolie companies_demo/companies_demo.ol`)

## Data Structure

```
companies.json:
  companies[0..10]
    └─ company
         ├─ name
         ├─ headquarters
         └─ departments[0..N]
              ├─ id
              ├─ name
              └─ teams[0..N]
                   ├─ team_id
                   ├─ team_name
                   └─ projects[0..N]
                        ├─ project_id
                        ├─ name
                        ├─ status (in_progress, planning, completed, testing)
                        └─ technologies[]
```

**4 LEVELS DEEP**: companies → departments → teams → projects

## WITHOUT TQUERY: 4 Nested Loops

```jolie
// Load data
readFile@File({ filename = "companies.json", format = "json" })(data);

resultCount = 0;
for (c = 0, c < #data.companies, c++) {                              // LOOP 1: companies
    company -> data.companies[c].company;

    for (d = 0, d < #company.departments, d++) {                     // LOOP 2: departments
        dept -> company.departments[d];

        for (t = 0, t < #dept.teams, t++) {                          // LOOP 3: teams
            team -> dept.teams[t];

            for (p = 0, p < #team.projects, p++) {                   // LOOP 4: projects
                project -> team.projects[p];

                if (project.status == "in_progress") {               // CHECK
                    result[resultCount] << project;
                    result[resultCount].company_name = company.name;
                    result[resultCount].team_name = team.team_name;
                    resultCount++
                }
            }
        }
    }
}

```

**Problems**:
- 4 nested loops (for → for → for → for)
- 1 if check scattered inside deepest loop
- Must traverse entire tree even if only 1 project matches
- Cumbersome to extend (e.g. what if we wanted to filter by technology as well?)
- Code scattered across 4 nesting levels

## WITH TQUERY: ONE Unwind + ONE Match

```jolie
// Load data
readFile@File({ filename = "companies.json", format = "json" })(data);

// ================================================================
// STEP 1: Flatten ALL 4 LEVELS with ONE unwind
// ================================================================
unwind@TQuery({
    data << data
    query = "companies.company.departments.teams.projects"
})(unwoundProjects);

/* HOW UNWIND WORKS:

   The query "companies.company.departments.teams.projects" tells TQuery:
   "Follow this path through the nested structure. Every time you encounter
   an array, create a separate result element for each array item, preserving
   the full path from root to that element.

*/

// ================================================================
// STEP 2: Filter by status = "in_progress" declaratively
// ================================================================
match@TQuery({
    data << unwoundProjects.result
    query.equal << {
        path = "companies.company.departments.teams.projects.status"
        data = "in_progress"
    }
})(filtered);

// ================================================================
// STEP 3: Direct copy to result w/out loops
// ================================================================
result << filtered.result;

```

```
WITHOUT TQUERY (imperative):
┌─────────────────────────────────────────┐
│ for companies                           │ ← Loop 1
│   ├─ for departments                    │ ← Loop 2
│   │   ├─ for teams                      │ ← Loop 3
│   │   │   ├─ for projects               │ ← Loop 4
│   │   │   │   └─ if status=="in_progress"│ ← Check (18 lines deep!)
│   │   │   │       ADD TO RESULT          │
└─────────────────────────────────────────┘
```

```
WITH TQUERY (declarative):
┌─────────────────────────────────────────┐
│ unwind "companies...projects"           │ ← Flatten 4 levels
│   ↓                                     │
│ match status == "in_progress"           │ ← Filter declaratively
│   ↓                                     │
│ result << filtered.result               │ ← Direct copy!
└─────────────────────────────────────────┘
```

## Advanced Example: Multiple Filters

(to run execute from top root dir `jolie companies_demo/companies_demo_advanced.ol`)


Want projects that are:
- Status: "in_progress"
- Technology includes: "Python"

```jolie
// STEP 1: Unwind (same as before)
unwind@TQuery({
    data << data
    query = "companies.company.departments.teams.projects"
})(unwoundProjects);

// STEP 2: Filter by status
match@TQuery({
    data << unwoundProjects.result
    query.equal << {
        path = "companies.company.departments.teams.projects.status"
        data = "in_progress"
    }
})(statusFiltered);

// STEP 3: Unwind technologies array
unwind@TQuery({
    data << statusFiltered.result
    query = "companies.company.departments.teams.projects.technologies"
})(unwoundTechs);

// STEP 4: Filter by technology
match@TQuery({
    data << unwoundTechs.result
    query.equal << {
        path = "companies.company.departments.teams.projects.technologies"
        data = "Python"
    }
})(result);
```


## **2) Problem**: make flexible-yet-typed API
(run `./AnyToArr/test.sh` from top root dir)

say client wanna send values, but very often it is either just one value or no value.
Structured approach would impose server to "add type annotation by marking field as, say, array of strings in the interface definition".
But it would be very cumbersome because:
+ add boilerplate & less readable code to client
```json
{
  "participants": ["John"]
}
```
*vs*
```json
{
  "participants": "John"
}
```

or

```json
{
  "participants": []
}
```
*vs*
```json
{
    // no field "participants". Intuitively, it means no participants...
}
```

+ *defensive programming* in server-side's code

<!-- servers need to normalize data in order to handle heterogeneous data in one single way (e.g. Any -> [string]), leading to **if-special-case programming** + **defensive programming** (e.g. check len of array before accessing it...) all the time! -->
e.g. check len of array (before accessing it...), check if the field is defined, etc all the time!

**WITHOUT TQuery**: Manual checks
```jolie
if (is_defined(request.tags)) {
    if (#request.tags == 1) {
        output.tags._ << request.tags  // Convert to array
    } else {
        output.tags << request.tags     // Copy array
    }
}
```

**WITH TQuery**: Automatic normalization
```jolie
unwind@TQuery({
    data << request
    query = "tags"
})(unwound);

for (i = 0, i < #unwound.result, i++) {
    output.tags[i] = unwound.result[i].tags
}
```

no matter what the client sent us, we get the same result in a more general way!

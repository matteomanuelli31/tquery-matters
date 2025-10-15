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
// STEP 1: Unwind to deepest level (technologies) - ONE unwind flattens ALL levels
unwind@TQuery({
    data << data
    query = "companies.company.departments.teams.projects.technologies"
})(unwoundTechs);

// STEP 2: Apply BOTH filters in ONE match using query.and
match@TQuery({
    data << unwoundTechs.result
    query.and << {
        left.equal << {
            path = "companies.company.departments.teams.projects.status"
            data = "in_progress"
        }
        right.equal << {
            path = "companies.company.departments.teams.projects.technologies"
            data = "Python"
        }
    }
})(result);
```

**Key Improvement**: Instead of 4 operations (unwind → match → unwind → match), we now use just **2 operations** (unwind → match with AND)

## **1c) Problem:** group scattered heterogeneous data from a nested structure

(run `jolie parameter_filtering.ol`)

### e.g. data Structure

```
paths[0]
  └─ methods[0] "get"
       └─ parameters[0] { in: "query", name: "page", type: "integer" }
       └─ parameters[1] { in: "header", name: "Authorization", type: "string" }
  └─ methods[1] "post"
       └─ parameters[0] { in: "body", name: "user", schema: {...} }
       └─ parameters[1] { in: "header", name: "Content-Type", type: "string" }

paths[1]
  └─ methods[0] "get"
       └─ parameters[0] { in: "path", name: "id", type: "string" }
       └─ parameters[1] { in: "header", name: "Authorization", type: "string" }
```


### WITHOUT TQUERY

```jolie
// GOAL: extract into {body, path, query, header}Params array


bodyCount = 0;
pathCount = 0;
queryCount = 0;
headerCount = 0;

for (p = 0, p < #data.paths, p++) {                          // LOOP 1: paths
    for (m = 0, m < #data.paths[p].methods, m++) {           // LOOP 2: methods
        for (par = 0, par < #data.paths[p].methods[m].parameters, par++) {  // LOOP 3: parameters
            param -> data.paths[p].methods[m].parameters[par];

            if (param.in == "body") {                         // CHECK 1
                bodyParams[bodyCount++] << param
            } else if (param.in == "path") {                  // CHECK 2
                pathParams[pathCount++] << param
            } else if (param.in == "query") {                 // CHECK 3
                queryParams[queryCount++] << param
            } else if (param.in == "header") {                // CHECK 4
                headerParams[headerCount++] << param
            }
        }
    }
}
```

**Problems**:
- 3 nested loops (for → for → for)
- 4 if/else conditionals
- Manual counter tracking (`bodyCount++`)
- Must traverse ALL parameters for EVERY check
- Scattered logic - each type mixed with others

### WITH TQUERY

```jolie
// STEP 1: Flatten ALL parameters from ALL methods from ALL paths
unwind@TQuery({
    data << data
    query = "paths.methods.parameters"
})(unwoundParams);
// Each record preserves FULL CONTEXT: path name, method name, AND parameter data.


// STEP 2a: Filter ONLY body parameters
match@TQuery({
    data << unwoundParams.result
    query.equal << {
        path = "paths.methods.parameters.in"
        data = "body"
    }
})(bodyParamsTQ);

// STEP 2b: Filter ONLY path parameters
match@TQuery({
    data << unwoundParams.result
    query.equal << {
        path = "paths.methods.parameters.in"
        data = "path"
    }
})(pathParamsTQ);

// STEP 2c: Filter ONLY query parameters
match@TQuery({
    data << unwoundParams.result
    query.equal << {
        path = "paths.methods.parameters.in"
        data = "query"
    }
})(queryParamsTQ);

// STEP 2d: Filter ONLY header parameters
match@TQuery({
    data << unwoundParams.result
    query.equal << {
        path = "paths.methods.parameters.in"
        data = "header"
    }
})(headerParamsTQ);

/* BONUS

// Instead of: if (param.in instanceof InBody)
// Use: Check if specific fields exist

// Get all parameters with schema field (body parameters)
match@TQuery({
  data << unwoundParams.result
  query.exists = "paths.methods.parameters.schema"
})(bodyParams);

(you can use AND filters)

*/

// STEP 3: Direct copy
bodyResult << bodyParamsTQ.result;
pathResult << pathParamsTQ.result;
queryResult << queryParamsTQ.result;
headerResult << headerParamsTQ.result;
```


### How Unwind Eliminates Triple Nesting

`query = "paths.methods.parameters"` flattens 3 levels in ONE operation

```
WITHOUT TQUERY:
for paths        ← Loop 1
  for methods    ← Loop 2
    for params   ← Loop 3
      process

WITH TQUERY:
unwind "paths.methods.parameters"  ← Flattens ALL 3 levels
→ Flat array ready for filtering
```

### How Match Eliminates Conditionals

`query.equal` selects matching records

```
WITHOUT TQUERY:
if (param.in == "body") {
    bodyParams[count++] << param
} else if (param.in == "path") {
    pathParams[count++] << param
}
// ... repeat for each type

WITH TQUERY:
match@TQuery({
    query.equal << { path = "...in", data = "body" }
})(bodyParams);

match@TQuery({
    query.equal << { path = "...in", data = "path" }
})(pathParams);
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

no matter what the client sent us, we get the same result in a more general way! (btw: the interface will declare `string*` as param type)

# Performance Benchmark

## Setup

A performance benchmark compares TQuery vs traditional nested loops vs JsonPath for filtering deeply nested JSON data.

**Dataset**: `large_data.json`
- 60 companies × 5 departments × 4 teams × 4 projects
- Total: 4,800 projects across 4 nesting levels
- Generated with `benchmark/generate_data.py`

**Verify dataset structure**:
```bash
python3 -c "
import json
with open('large_data.json') as f:
    data = json.load(f)

companies = len(data['companies'])
total_depts = sum(len(c['company']['departments']) for c in data['companies'])
total_teams = sum(len(d['teams']) for c in data['companies'] for d in c['company']['departments'])
total_projects = sum(len(t['projects']) for c in data['companies'] for d in c['company']['departments'] for t in d['teams'])

print(f'Companies: {companies}')
print(f'Departments: {total_depts}')
print(f'Teams: {total_teams}')
print(f'Projects: {total_projects}')
print(f'Avg departments per company: {total_depts / companies:.1f}')
print(f'Avg teams per department: {total_teams / total_depts:.1f}')
print(f'Avg projects per team: {total_projects / total_teams:.1f}')
"
```

**Test Query**: Filter projects by `status` AND `technology`
- Example: `{status: "in_progress", technology: "Python"}`
- Tests concurrent requests with varying load

**Three Implementations**:
1. **WITHOUT TQuery**: Traditional nested loops in Jolie
2. **WITH TQuery**: Declarative filtering using TQuery
3. **JsonPath**: Java implementation using JsonPath library

## System Information

```
CPU: Intel(R) Core(TM) 7 150U
Memory: 15 GiB total
OS: Debian GNU/Linux (kernel 6.1.0-40-amd64)
Java: OpenJDK 64-Bit Server VM (build 21.0.8+9-Debian-1)
```

## Prerequisites

```bash
# Install dependencies (first time only)
python3 -m venv venv
venv/bin/pip install aiohttp

# Generate test data (first time only)
python3 benchmark/generate_data.py

# Build JsonPath server (first time only)
cd benchmark && gradle build && cd ..
```

## How to Run

```bash
# Run benchmark with N parallel requests
venv/bin/python3 benchmark/run_all_benchmarks.py <N>

# Examples
venv/bin/python3 benchmark/run_all_benchmarks.py 5
venv/bin/python3 benchmark/run_all_benchmarks.py 7
```

**Output**: Summary table with performance metrics

```
======================================================================
BENCHMARK SUMMARY
======================================================================
Metric               WITHOUT TQuery       WITH TQuery          JsonPath
----------------------------------------------------------------------
P50 (ms)             335                  3886                 190
P95 (ms)             339                  3909                 237
Max Heap (MB)        134.0                3917.1               5.4
Young GC             6                    35                   1
Full GC              0                    1                    0
======================================================================
```

**Java Heap Memory Structure:**

The heap is divided into generational spaces based on object lifetime:

```
┌─────────────────────────────────────────────────────────────┐
│                         Java Heap                           │
├──────────────────────────────────┬──────────────────────────┤
│        Young Generation          │    Old Generation        │
├───────────┬───────────┬──────────┤                          │
│   Eden    │ Survivor0 │ Survivor1│         Old              │
│           │   (S0)    │   (S1)   │                          │
└───────────┴───────────┴──────────┴──────────────────────────┘
```

- **Eden**: Where new objects are allocated. Most objects die young (short-lived)
- **Survivor0 & Survivor1 (S0/S1)**: Objects that survive one minor GC move here. Only one survivor space is active at a time
- **Old Generation**: Long-lived objects promoted after surviving multiple GCs. Collected less frequently (major GC = expensive)

**Max Heap (MB) shows:** Total heap usage = S0 + S1 + Eden + Old (excludes Metaspace which stores class metadata)

### Command Explanations

**Java Command Breakdown:**
```bash
java -Xlog:gc*:file=/tmp/gc.log:time,uptime,level,tags \
    -cp /usr/lib/jolie/lib/libjolie.jar:... \
    jolie.Jolie \
    -l ./lib/*:... \
    -i /usr/lib/jolie/include \
    -p /usr/lib/jolie/packages \
    benchmark/server.ol
```

**Performance monitoring:**
- `-Xlog:gc*:file=/tmp/gc.log:time,uptime,level,tags`: Detailed GC logging
- Use `jstat` (JDK tool) for GC statistics: `jstat -gcutil <pid>` or `jstat -gccause <pid>`

**Optional (for testing memory pressure):**
- `-Xmx500m`: Maximum heap size = 500 MB
- `-Xms500m`: Initial heap size = 500 MB

**Runtime:**
- `-cp /usr/lib/jolie/lib/...`: Java classpath. Specifies where to find compiled Java classes and JAR libraries.
- `jolie.Jolie`: The fully-qualified name of the main Java class to execute.
- `-l ./lib/*:...`: Jolie library paths. Tells Jolie where to find additional libraries and extensions.
- `-i /usr/lib/jolie/include`: Jolie include directory. Where Jolie looks for `.iol` interface files.
- `-p /usr/lib/jolie/packages`: Jolie packages directory. Where Jolie looks for installed packages (e.g., `@jolie/tquery`).
- `benchmark/server.ol`: The Jolie source file to execute.

**GC Statistics with jstat:**

The benchmark script uses `jstat` (JDK built-in tool) to extract GC statistics automatically. It shows:
- `jstat -gcutil <pid>`: Heap utilization percentages (S0, S1, Eden, Old, Metaspace), GC counts, and GC time
- `jstat -gccause <pid>`: Same as above plus the cause of the last GC event

Raw GC logs are available in `/tmp/gc.log` for detailed manual analysis if needed.

## Metrics Explained

**Latency Percentiles**:
- **P50 (median)**: 50% of requests completed faster than this time
- **P95**: 95% of requests completed faster than this time (only 5% were slower)

**Memory & GC**:
- **Max Heap (MB)**: Maximum heap memory used (S0 + S1 + Eden + Old Generation)
- **Young GC**: Minor garbage collection count (short-lived objects)
- **Full GC**: Major garbage collection count (entire heap, expensive)

## Benchmark Results

### Test 1: 5 parallel requests

```
======================================================================
BENCHMARK SUMMARY
======================================================================
Metric               WITHOUT TQuery       WITH TQuery          JsonPath
----------------------------------------------------------------------
P50 (ms)             335                  3886                 190
P95 (ms)             339                  3909                 237
Max Heap (MB)        134.0                3917.1               5.4
Young GC             6                    35                   1
Full GC              0                    1                    0
======================================================================
```

### Test 2: 7 parallel requests

```
======================================================================
BENCHMARK SUMMARY
======================================================================
Metric               WITHOUT TQuery       WITH TQuery          JsonPath
----------------------------------------------------------------------
P50 (ms)             356                  5242                 284
P95 (ms)             366                  5269                 364
Max Heap (MB)        141.6                3919.1               5.4
Young GC             6                    42                   1
Full GC              0                    2                    0
======================================================================
```

## Simple JsonPath Example

**Purpose**: Demonstrates JsonPath's ability to filter on fields that belong to non-overlapping trees in a single query.

**Files**:
- `test_data.json`: 2 entries, each with two nested trees (tree1: person data, tree2: product data)
- `TestFilter.java`: Simple program with two JsonPath filters

**How to run**:
```bash
cd benchmark && gradle run -PmainClass=TestFilter
```

**What it does**:
- **Filter 1**: `age > 28 && price > 1000` → matches Alice/Laptop
- **Filter 2**: `age < 28 && price < 1000` → matches Bob/Phone

This example shows how JsonPath can combine conditions across different nested structures (`tree1.details.age` and `tree2.specs.price`) in a single declarative query.

Can `Tquery` achieve this?

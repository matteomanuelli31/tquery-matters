## get tquery

### prerequisites
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

## use cases where tquery would save our bacon

(see working example [here](nasty-nested.ol))
**Problem:** filter all operations s.t.: method == "get" AND requiresAuth == true AND "admin" in tags. simple - isn't it?

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


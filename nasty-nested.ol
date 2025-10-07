from @jolie.tquery.main import TQuery
from console import Console

service TestNestedFiltering {
    embed TQuery as TQuery
    embed Console as Console

    main {
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

        println@Console("=== WITHOUT TQUERY ===")()
        for (i = 0, i < #apiData.operations, i++) {
            if (apiData.operations[i].method == "get") {
                if (apiData.operations[i].requiresAuth == true) {
                    hasAdminTag = false;
                    for (j = 0, j < #apiData.operations[i].tags, j++) {
                        if (apiData.operations[i].tags[j] == "admin") {
                            hasAdminTag = true
                        }
                    };
                    if (hasAdminTag) {
                        result1[#result1] << apiData.operations[i]
                    }
                }
            }
        };

        for (i = 0, i < #result1, i++) {
            println@Console(result1[i].method + " " + result1[i].path)()
        };

        println@Console("\n=== WITH TQUERY ===")()
        unwind@TQuery({
            data << apiData
            query = "operations"
        })(unwoundOps);

        match@TQuery({
            data << unwoundOps.result
            query.and << {
                left.equal << { path = "operations.method" data = "get" }
                right.equal << { path = "operations.requiresAuth" data = true }
            }
        })(filteredOps);

        unwind@TQuery({
            data << filteredOps.result
            query = "operations.tags"
        })(unwoundTags);

        match@TQuery({
            data << unwoundTags.result
            query.equal << { path = "operations.tags" data = "admin" }
        })(result2);

        for (i = 0, i < #result2.result, i++) {
            op -> result2.result[i].operations;
            println@Console(op.method + " " + op.path)()
        }
    }
}

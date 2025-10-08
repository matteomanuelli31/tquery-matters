from @jolie.tquery.main import TQuery
from console import Console

service TestTripleNestingToOne {
    embed TQuery as TQuery
    embed Console as Console

    main {
        request.paths[0] << {
            pathName = "/users"
            methods[0] << {
                methodName = "get"
                parameters[0] << { name = "id", in = "path", type = "integer", required = true }
                parameters[1] << { name = "filter", in = "query", type = "string", required = false }
            };
            methods[1] << {
                methodName = "post"
                parameters[0] << { name = "body", in = "body", schemaType = "User", required = true }
            }
        };
        request.paths[1] << {
            pathName = "/posts"
            methods[0] << {
                methodName = "get"
                parameters[0] << { name = "page", in = "query", type = "integer", required = false }
                parameters[1] << { name = "auth", in = "header", type = "string", required = true }
            };
            methods[1] << {
                methodName = "delete"
                parameters[0] << { name = "id", in = "path", type = "integer", required = true }
                parameters[1] << { name = "body", in = "body", schemaRef = "#/definitions/DeleteRequest", required = true }
            }
        };

        println@Console("=== WITHOUT TQUERY ===")();

        for(pathIdx = 0, pathIdx < #request.paths, pathIdx++) {
            pathData -> request.paths[pathIdx];
            for(methodIdx = 0, methodIdx < #pathData.methods, methodIdx++) {
                methodData -> pathData.methods[methodIdx];
                for(par = 0, par < #methodData.parameters, par++) {
                    param -> methodData.parameters[par];

                    if (param.in == "body") {
                        result1[#result1] = pathData.pathName + " " +
                            methodData.methodName + " -> " + param.name
                    } else if (param.in == "path") {
                        result1[#result1] = pathData.pathName + " " +
                            methodData.methodName + " -> " + param.name
                    }
                }
            }
        };

        for (i = 0, i < #result1, i++) {
            println@Console(result1[i])()
        };

        println@Console("\n=== WITH TQUERY ===")();

        unwind@TQuery({
            data << request
            query = "paths.methods.parameters"
        })(unwoundAll);

        match@TQuery({
            data << unwoundAll.result
            query.equal << {
                path = "paths.methods.parameters.in"
                data = "body"
            }
        })(bodyParams);

        match@TQuery({
            data << unwoundAll.result
            query.equal << {
                path = "paths.methods.parameters.in"
                data = "path"
            }
        })(pathParams);

        bodyResult << bodyParams.result;
        pathResult << pathParams.result;

        for (i = 0, i < #bodyResult, i++) {
            p -> bodyResult[i].paths;
            result2[#result2] = p.pathName + " " +
                p.methods.methodName + " -> " +
                p.methods.parameters.name
        };

        for (i = 0, i < #pathResult, i++) {
            p -> pathResult[i].paths;
            result2[#result2] = p.pathName + " " +
                p.methods.methodName + " -> " +
                p.methods.parameters.name
        };

        for (i = 0, i < #result2, i++) {
            println@Console(result2[i])()
        }
    }
}

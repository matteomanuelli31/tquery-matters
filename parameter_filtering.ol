from file import File
from console import Console
from @jolie.tquery.main import TQuery

service ParameterFilteringDemo {
    embed File as File
    embed Console as Console
    embed TQuery as TQuery

    main {
        readFile@File({
            filename = "openapi_parameters.json"
            format = "json"
        })(data);

        println@Console("\n=== WITHOUT TQUERY ===\n")();

        bodyCount = 0;
        pathCount = 0;
        queryCount = 0;
        headerCount = 0;

        for (p = 0, p < #data.paths, p++) {
            for (m = 0, m < #data.paths[p].methods, m++) {
                for (par = 0, par < #data.paths[p].methods[m].parameters, par++) {
                    param -> data.paths[p].methods[m].parameters[par];

                    if (param.in == "body") {
                        bodyParams[bodyCount++] << param
                    } else if (param.in == "path") {
                        pathParams[pathCount++] << param
                    } else if (param.in == "query") {
                        queryParams[queryCount++] << param
                    } else if (param.in == "header") {
                        headerParams[headerCount++] << param
                    }
                }
            }
        };

        println@Console("  body: " + bodyCount)();
        println@Console("  path: " + pathCount)();
        println@Console("  query: " + queryCount)();
        println@Console("  header: " + headerCount)();

        println@Console("\n=== WITH TQUERY ===\n")();

        unwind@TQuery({
            data << data
            query = "paths.methods.parameters"
        })(unwoundParams);

        match@TQuery({
            data << unwoundParams.result
            query.equal << {
                path = "paths.methods.parameters.in"
                data = "body"
            }
        })(bodyParamsTQ);

        match@TQuery({
            data << unwoundParams.result
            query.equal << {
                path = "paths.methods.parameters.in"
                data = "path"
            }
        })(pathParamsTQ);

        match@TQuery({
            data << unwoundParams.result
            query.equal << {
                path = "paths.methods.parameters.in"
                data = "query"
            }
        })(queryParamsTQ);

        match@TQuery({
            data << unwoundParams.result
            query.equal << {
                path = "paths.methods.parameters.in"
                data = "header"
            }
        })(headerParamsTQ);

        bodyResult << bodyParamsTQ.result;
        pathResult << pathParamsTQ.result;
        queryResult << queryParamsTQ.result;
        headerResult << headerParamsTQ.result;

        println@Console("  bodyResult: " + #bodyResult)();
        println@Console("  pathResult: " + #pathResult)();
        println@Console("  queryResult: " + #queryResult)();
        println@Console("  headerResult: " + #headerResult)()
    }
}

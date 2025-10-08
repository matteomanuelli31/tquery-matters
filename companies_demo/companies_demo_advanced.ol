from file import File
from console import Console
from @jolie.tquery.main import TQuery

service CompaniesDemoAdvanced {
    embed File as File
    embed Console as Console
    embed TQuery as TQuery

    main {
        readFile@File({
            filename = "companies.json"
            format = "json"
        })(data);

        println@Console("\n=== In-Progress Python Projects ===\n")();

        unwind@TQuery({
            data << data
            query = "companies.company.departments.teams.projects"
        })(unwoundProjects);

        match@TQuery({
            data << unwoundProjects.result
            query.equal << {
                path = "companies.company.departments.teams.projects.status"
                data = "in_progress"
            }
        })(statusFiltered);

        unwind@TQuery({
            data << statusFiltered.result
            query = "companies.company.departments.teams.projects.technologies"
        })(unwoundTechs);

        match@TQuery({
            data << unwoundTechs.result
            query.equal << {
                path = "companies.company.departments.teams.projects.technologies"
                data = "Python"
            }
        })(result);

        for (i = 0, i < #result.result, i++) {
            proj -> result.result[i].companies.company.departments.teams.projects;
            compName = result.result[i].companies.company.name;

            println@Console("  - " + proj.project_id + ": " + proj.name + " (" + compName + ")")()
        }
    }
}

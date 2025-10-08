from file import File
from console import Console
from @jolie.tquery.main import TQuery

service CompaniesDemoService {
    embed File as File
    embed Console as Console
    embed TQuery as TQuery

    main {
        readFile@File({
            filename = "companies.json"
            format = "json"
        })(data);

        println@Console("\n=== WITHOUT TQUERY ===\n")();

        resultCount = 0;
        for (c = 0, c < #data.companies, c++) {
            company -> data.companies[c].company;

            for (d = 0, d < #company.departments, d++) {
                dept -> company.departments[d];

                for (t = 0, t < #dept.teams, t++) {
                    team -> dept.teams[t];

                    for (p = 0, p < #team.projects, p++) {
                        project -> team.projects[p];

                        if (project.status == "in_progress") {
                            withoutTQuery[resultCount] << project;
                            withoutTQuery[resultCount].company_name = company.name;
                            withoutTQuery[resultCount].team_name = team.team_name;
                            resultCount++
                        }
                    }
                }
            }
        };

        for (i = 0, i < #withoutTQuery, i++) {
            println@Console("  - " + withoutTQuery[i].project_id + ": " +
                          withoutTQuery[i].name + " (" +
                          withoutTQuery[i].company_name + " / " +
                          withoutTQuery[i].team_name + ")")()
        };

        println@Console("\n=== WITH TQUERY ===\n")();

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
        })(filtered);

        withTQuery << filtered.result;

        for (i = 0, i < #withTQuery, i++) {
            proj -> withTQuery[i].companies.company.departments.teams.projects;
            compName = withTQuery[i].companies.company.name;
            teamName = withTQuery[i].companies.company.departments.teams.team_name;

            println@Console("  - " + proj.project_id + ": " +
                          proj.name + " (" +
                          compName + " / " +
                          teamName + ")")()
        }
    }
}

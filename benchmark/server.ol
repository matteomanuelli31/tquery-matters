include "interface.iol"
from @jolie.tquery.main import TQuery
from file import File
from console import Console

service FilterServer {
    embed TQuery as TQuery
    embed File as File
    embed Console as Console

    execution { concurrent }

    inputPort FilterPort {
        Location: "socket://localhost:9000"
        Protocol: http {
            .format = "json"
        }
        Interfaces: FilterInterface
    }

    init {
        println@Console("Loading data from large_data.json...")();
        readFile@File({
            filename = "benchmark/large_data.json"
            format = "json"
        })(global.data);
        println@Console("Data loaded successfully!")()
    }

    main {
        filterProjects(request)(response) {
            if (request.useTQuery) {
                // WITH TQUERY: One unwind + one match
                unwind@TQuery({
                    data << global.data
                    query = "companies.company.departments.teams.projects.technologies"
                })(unwoundTechs);

                match@TQuery({
                    data << unwoundTechs.result
                    query.and << {
                        left.equal << {
                            path = "companies.company.departments.teams.projects.status"
                            data = request.status
                        }
                        right.equal << {
                            path = "companies.company.departments.teams.projects.technologies"
                            data = request.technology
                        }
                    }
                })(filtered);

                response.count = #filtered.result;
                for (i = 0, i < #filtered.result, i++) {
                    proj -> filtered.result[i].companies.company.departments.teams.projects;
                    response.results[i] << {
                        project_id = proj.project_id
                        name = proj.name
                        status = proj.status
                        budget = proj.budget
                        start_date = proj.start_date
                    };
                    for (t = 0, t < #proj.technologies, t++) {
                        response.results[i].technologies[t] = proj.technologies[t]
                    }
                }
            } else {
                // WITHOUT TQUERY: Nested loops
                resultCount = 0;
                for (c = 0, c < #global.data.companies, c++) {
                    company -> global.data.companies[c].company;
                    for (d = 0, d < #company.departments, d++) {
                        dept -> company.departments[d];
                        for (t = 0, t < #dept.teams, t++) {
                            team -> dept.teams[t];
                            for (p = 0, p < #team.projects, p++) {
                                project -> team.projects[p];
                                if (project.status == request.status) {
                                    hasTech = false;
                                    for (tech = 0, tech < #project.technologies && !hasTech, tech++) {
                                        if (project.technologies[tech] == request.technology) {
                                            hasTech = true
                                        }
                                    };
                                    if (hasTech) {
                                        response.results[resultCount] << {
                                            project_id = project.project_id
                                            name = project.name
                                            status = project.status
                                            budget = project.budget
                                            start_date = project.start_date
                                        };
                                        for (tech = 0, tech < #project.technologies, tech++) {
                                            response.results[resultCount].technologies[tech] = project.technologies[tech]
                                        };
                                        resultCount++
                                    }
                                }
                            }
                        }
                    }
                };
                response.count = resultCount
            }
        }
    }
}

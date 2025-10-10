type FilterRequest {
    status: string
    technology: string
    useTQuery: bool
}

type ProjectResult {
    project_id: string
    name: string
    status: string
    budget: int
    start_date: string
    technologies*: string
}

type FilterResponse {
    count: int
    results*: ProjectResult
}

interface FilterInterface {
    RequestResponse: filterProjects(FilterRequest)(FilterResponse)
}

type TagRequest {
    name: string
    tags*: string
}

type TagResponse {
    name: string
    normalizedTags[0,*]: string
}

interface TagNormalizationInterface {
    RequestResponse: normalizeTags(TagRequest)(TagResponse)
}

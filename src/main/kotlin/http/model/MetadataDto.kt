package http.model

data class MetadataDto(val artifactId: String, val groupId: String, val versions: List<String>)

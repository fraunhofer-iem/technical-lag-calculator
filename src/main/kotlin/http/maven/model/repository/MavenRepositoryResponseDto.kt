package http.maven.model.repository

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@XmlSerialName("metadata")
@Serializable
data class MavenMetadata(
    @XmlElement(true) @SerialName("groupId")
    val groupId: String? = null,

    @XmlElement(true) @SerialName("artifactId")
    val artifactId: String? = null,

    @XmlElement(true) @SerialName("versioning")
    val versioning: Versioning? = null,

    @XmlElement(true) @SerialName("version")
    val version: String? = null,

    @XmlElement(true) @SerialName("plugins")
    val plugins: Plugins? = null,

    @XmlElement(false) @SerialName("modelVersion")
    val modelVersion: String? = null
)

@XmlSerialName("plugins")
@Serializable
data class Plugins(
    @XmlElement(true) @SerialName("plugin")
    val plugin: List<Plugin> = emptyList()
)

@XmlSerialName("plugin")
@Serializable
data class Plugin(
    @XmlElement(true) @SerialName("name")
    val name: String? = null,
    @XmlElement(true) @SerialName("prefix")
    val prefix: String? = null,
    @XmlElement(true) @SerialName("pluginArtifactId")
    val pluginArtifactId: String? = null
)

@XmlSerialName("versioning")
@Serializable
data class Versioning(
    @XmlElement(true)
    @SerialName("latest")
    val latest: String? = null,
    @XmlElement(true)
    @SerialName("release")
    val release: String? = null,
    @XmlElement(true)
    @SerialName("versions")
    val versions: Versions? = null,
    @XmlElement(true)
    @SerialName("lastUpdated")
    val lastUpdated: String? = null,
    @XmlElement(true)
    @SerialName("snapshot")
    val snapshot: Snapshot? = null,
    @XmlElement(true)
    @SerialName("snapshotVersions")
    val snapshotVersions: SnapshotVersions? = null
)

@XmlSerialName("Snapshot")
@Serializable
data class Snapshot(
    @XmlElement(true)
    @SerialName("timestamp")
    val timestamp: String? = null,
    @XmlElement(true)
    @SerialName("buildNumber")
    val buildNumber: Int? = null,
    @XmlElement(true)
    @SerialName("localCopy")
    val localCopy: Boolean? = null
)

@XmlSerialName("SnapshotVersions")
@Serializable
data class SnapshotVersions(
    @XmlElement(true)
    @SerialName("snapshotVersion")
    val snapshotVersion: List<SnapshotVersion> = emptyList()
)

@XmlSerialName("SnapshotVersion")
@Serializable
data class SnapshotVersion(
    @XmlElement(true)
    @SerialName("classifier")
    val classifier: String? = null,
    @XmlElement(true)
    @SerialName("extension")
    val extension: String? = null,
    @XmlElement(true)
    @SerialName("value")
    val value: String? = null,
    @XmlElement(true)
    @SerialName("updated")
    val updated: String? = null
)

@XmlSerialName("versions")
@Serializable
data class Versions(
    @XmlElement(true)
    @SerialName("version")
    val version: List<String> = emptyList()
)

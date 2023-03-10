rootProject.name = "backend"

val projectDirs = listOf(
    "shared",
    "services",
)

projectDirs.forEach {
    file(it).listFiles {file ->
        file.isDirectory
    }?.forEach { dir ->
        includeBuild (dir)
    }
}

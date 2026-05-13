buildCache.registerMojoMetadataProvider(context -> {
    context.withPlugin("maven-checkstyle-plugin", () -> {
        context.inputs(inputs -> {
            inputs.fileSet("testSourceDirectories", fileSet ->
                fileSet.excludes("**/target/**")
            )
        })
    })
})

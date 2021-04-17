/**
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Modifications copyright 2021 Anthony Marland
 */

package com.google.android.gms.oss.licenses.plugin

import groovy.json.JsonBuilder
import groovy.json.JsonException
import groovy.json.JsonSlurper
import groovy.xml.QName
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.component.AmbiguousVariantSelectionException
import org.slf4j.LoggerFactory

/**
 * This task does the following:
 * First it finds all dependencies that meet all the requirements below:
 * 1. Can be resolved.
 * 2. Not test compile.
 * Then it finds all the artifacts associated with the dependencies.
 * Finally it generates a json file that contains the information about these
 * artifacts.
 */
class DependencyTask extends DefaultTask {
    protected Set<String> artifactSet = []
    // Changed Apr 11 2021: field `artifactInfos` renamed `artifactInfoSet`
    protected Set<ArtifactInfo> artifactInfoSet = []
    protected static final String LOCAL_LIBRARY_VERSION = "unspecified"
    private static final String TEST_PREFIX = "test"
    /* Changed Apr 11 2021: commented out unneeded Android-specific fields
    private static final String ANDROID_TEST_PREFIX = "androidTest"
    private static final Set<String> TEST_COMPILE = ["testCompile",
                                                     "androidTestCompile"]
    */
    // Added Apr 11 2021: changed type of field `TEST_COMPILE` to `String`
    private static final String TEST_COMPILE = "testCompile"

    private static final Set<String> PACKAGED_DEPENDENCIES_PREFIXES = ["compile",
                                                                       "implementation",
                                                                       "api"]

    private static final logger = LoggerFactory.getLogger(DependencyTask.class)

    private Project project

    @OutputDirectory
    File outputDir

    @OutputFile
    File outputFile

    /**
     * Returns a serializable snapshot of direct dependencies from all relevant
     * configurations to allow task caching. To improve performance, this does
     * not resolve transitive dependencies. Direct dependencies should require a
     * version bump to publish a new POM file with updated transitive
     * dependencies.
     */
    @Input
    List<String> getDirectDependencies() {
        return collectDependenciesFromConfigurations(
                project.getConfigurations(),
                [project] as Set<Project>
        )
    }

    protected List<String> collectDependenciesFromConfigurations(
            ConfigurationContainer configurationContainer,
            Set<Project> visitedProjects
    ) {
        Set<String> directDependencies = new HashSet<>()
        Set<Project> libraryProjects = new HashSet<>()
        for (Configuration configuration in configurationContainer) {
            if (shouldSkipConfiguration(configuration)) {
                continue
            }

            for (Dependency dependency in configuration.allDependencies) {
                if (dependency instanceof ProjectDependency) {
                    libraryProjects.add(dependency.getDependencyProject())
                } else if (dependency instanceof ExternalModuleDependency) {
                    directDependencies.add(toMavenId(dependency))
                }
            }
        }
        for (Project libraryProject in libraryProjects) {
            if (libraryProject in visitedProjects) {
                continue
            }
            visitedProjects.add(libraryProject)
            // Changed Apr 11 2021: added period to log message and removed unnecessary braces
            logger.info("Visiting dependency $libraryProject.displayName.")
            directDependencies.addAll(
                    collectDependenciesFromConfigurations(
                            libraryProject.getConfigurations(),
                            visitedProjects
                    )
            )
        }
        return directDependencies.sort()
    }

    protected static String toMavenId(Dependency dependency) {
        return "${dependency.getGroup()}:${dependency.getName()}:${dependency.getVersion()}"
    }

    @TaskAction
    void action() {
        initOutput()
        updateDependencyArtifacts()

        if (outputFile.exists() && checkArtifactSet(outputFile)) {
            return
        }

        outputFile.newWriter()
        outputFile.write(new JsonBuilder(artifactInfoSet).toPrettyString())
    }

    // Changed Apr 11 2021: formatted Javadoc comment
    /**
     * Checks if current artifact set is the same as the artifact set in the json file.
     * @param file
     * @return true if artifactSet is the same as the json file, false otherwise
     */
    protected boolean checkArtifactSet(File file) {
        Set<String> artifacts = new HashSet<>(artifactSet)
        try {
            def previousArtifacts = new JsonSlurper().parse(file)
            for (entry in previousArtifacts) {
                String key = "${entry.fileLocation}"
                if (artifacts.contains(key)) {
                    artifacts.remove(key)
                } else {
                    return false
                }
            }
            return artifacts.isEmpty()
        } catch (JsonException ignored) { // Changed Apr 11 2021: variable `exception` renamed `ignored`
            return false
        }
    }

    protected void updateDependencyArtifacts() {
        for (Configuration configuration : project.getConfigurations()) {
            Set<ResolvedArtifact> artifacts = getResolvedArtifacts(configuration)
            if (artifacts == null) {
                continue
            }

            addArtifacts(artifacts)
        }
    }

    protected addArtifacts(Set<ResolvedArtifact> artifacts) {
        for (ResolvedArtifact artifact : artifacts) {
            String group = artifact.moduleVersion.id.group
            String artifact_key = artifact.file.getAbsolutePath()

            // Commented out Apr 13 2021: if (artifactSet.contains(artifact_key)) {
            if (!artifactSet.add(artifact_key)) {
                continue
            }

            // Commented out Apr 13 2021: artifactSet.add(artifact_key)
            artifactInfoSet.add(
                    new ArtifactInfo(group, artifact.name,
                            artifact.file.getAbsolutePath(),
                            artifact.moduleVersion.id.version)
            )
        }
    }

    // Changed Apr 11 2021: method `canBeResolved` removed because backwards compatibility
    //                      was not deemed necessary

    // Changed Apr 11 2021: - mention of "androidTestCompile" removed from Javadoc comment
    //                      - method made static
    /**
     * Checks if the configuration is from test.
     * @param configuration
     * @return true if configuration is a test configuration or its parent
     * configurations are either testCompile, otherwise false.
     */
    protected static boolean isTest(Configuration configuration) {
        // Changed Apr 11 2021: removed reference to now non-existent field `ANDROID_TEST_PREFIX`
        boolean isTestConfiguration = configuration.name.startsWith(TEST_PREFIX)
        configuration.hierarchy.each {
            isTestConfiguration |= TEST_COMPILE.contains(it.name)
        }
        return isTestConfiguration
    }

    // Changed Apr 11 2021: method made static, added period to Javadoc comment
    /**
     * Checks if the configuration is for a packaged dependency (rather than e.g. a build or test time dependency).
     * @param configuration
     * @return true if the configuration is in the set of @link #BINARY_DEPENDENCIES
     */
    protected static boolean isPackagedDependency(Configuration configuration) {
        boolean isPackagedDependency = PACKAGED_DEPENDENCIES_PREFIXES.any {
            configuration.name.startsWith(it)
        }
        configuration.hierarchy.each {
            String configurationHierarchyName = it.name
            isPackagedDependency |= PACKAGED_DEPENDENCIES_PREFIXES.any {
                configurationHierarchyName.startsWith(it)
            }
        }

        return isPackagedDependency
    }

    // Changed Apr 11 2021: made the method signature a one-liner, removed the extra line break
    protected Set<ResolvedArtifact> getResolvedArtifacts(Configuration configuration) {
        if (shouldSkipConfiguration(configuration)) {
            return null
        }

        try {
            return getResolvedArtifactsFromResolvedDependencies(
                    configuration.getResolvedConfiguration()
                            .getLenientConfiguration()
                            .getFirstLevelModuleDependencies())
        } catch (ResolveException exception) {
            logger.warn("Failed to resolve OSS licenses for $configuration.name.", exception)
            return null
        }
    }

    // Changed Apr 11 2021: - removed mention of Gradle API backward compatibility in Javadoc comment
    //                      - method made static
    /**
     * Returns true for configurations that cannot be resolved, are tests,
     * or are not packaged dependencies.
     */
    private static boolean shouldSkipConfiguration(Configuration configuration) {
        (!configuration.isCanBeResolved()
                || isTest(configuration)
                || !isPackagedDependency(configuration))
    }

    protected Set<ResolvedArtifact> getResolvedArtifactsFromResolvedDependencies(
            Set<ResolvedDependency> resolvedDependencies) {

        HashSet<ResolvedArtifact> resolvedArtifacts = new HashSet<>()
        for (resolvedDependency in resolvedDependencies) {
            try {
                if (resolvedDependency.getModuleVersion() == LOCAL_LIBRARY_VERSION) {
                    /**
                     * Attempting to getAllModuleArtifacts on a local library project will result
                     * in AmbiguousVariantSelectionException as there are not enough criteria
                     * to match a specific variant of the library project. Instead we skip the
                     * the library project itself and enumerate its dependencies.
                     */
                    resolvedArtifacts.addAll(
                            getResolvedArtifactsFromResolvedDependencies(
                                    resolvedDependency.getChildren()))
                } else {
                    resolvedArtifacts.addAll(resolvedDependency.getAllModuleArtifacts())
                }
            } catch (AmbiguousVariantSelectionException exception) {
                // Changed Apr 11 2021: added period to log message
                logger.warn("Failed to process $resolvedDependency.name.", exception)
            }
        }
        return resolvedArtifacts
    }

    private void initOutput() {
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
    }

    void setProject(Project project) {
        this.project = project
    }
}

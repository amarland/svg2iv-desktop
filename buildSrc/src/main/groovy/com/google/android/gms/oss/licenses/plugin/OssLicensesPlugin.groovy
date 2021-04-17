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

import org.gradle.api.Plugin
import org.gradle.api.Project

class OssLicensesPlugin implements Plugin<Project> {
    void apply(Project project) {
        // Changed Apr 11 2021: moved the second argument to the same line as the first one
        def getDependencies = project.tasks.create("getDependencies", DependencyTask)
        // Changed Apr 11 2021: moved the second argument to the same line as the first one
        def dependencyOutput = new File(project.buildDir, "generated/third_party_licenses")
        def generatedJson = new File(dependencyOutput, "dependencies.json")
        getDependencies.setProject(project)
        getDependencies.outputDir = dependencyOutput
        getDependencies.outputFile = generatedJson

        // Changed Apr 11 2021: changed second argument from "/res"
        def resourceOutput = new File(dependencyOutput, "/resources")
        def outputDir = new File(resourceOutput, "/raw")
        def licensesFile = new File(outputDir, "third_party_licenses")
        // Changed Apr 11 2021: moved the second argument to the same line as the first one
        def licensesMetadataFile = new File(outputDir, "third_party_license_metadata")
        // Changed Apr 11 2021: renamed variable from `licensesTask` to match the class name
        def licensesTask = project.tasks.create("generateLicenses", LicensesTask)

        licensesTask.dependenciesJson = generatedJson
        licensesTask.outputDir = outputDir
        licensesTask.licenses = licensesFile
        licensesTask.licensesMetadata = licensesMetadataFile

        licensesTask.inputs.file(generatedJson)
        licensesTask.outputs.dir(outputDir)
        licensesTask.outputs.files(licensesFile, licensesMetadataFile)

        licensesTask.dependsOn(getDependencies)

        // Changed Apr 11 2021: removed Android-specific block and replaced it with the following:
        project.tasks.findByName("assemble").dependsOn(licensesTask)

        // Changed Apr 11 2021: - task was renamed from "licensesCleanUp"
        //                      - moved the second argument to the same line as the first one
        def cleanupTask = project.tasks.create("licensesCleanup", LicensesCleanupTask)
        cleanupTask.dependencyFile = generatedJson
        cleanupTask.dependencyDir = dependencyOutput
        cleanupTask.licensesFile = licensesFile
        cleanupTask.metadataFile = licensesMetadataFile
        cleanupTask.licensesDir = outputDir

        project.tasks.findByName("clean").dependsOn(cleanupTask)
    }
}

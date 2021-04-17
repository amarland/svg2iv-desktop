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

// Changed Apr 11 2021: - all fields made final

package com.google.android.gms.oss.licenses.plugin

class ArtifactInfo {
    private final String group
    private final String name
    private final String fileLocation
    private final String version

    ArtifactInfo(String group,
                 String name,
                 String fileLocation,
                 String version) {
        this.group = group
        this.name = name
        this.fileLocation = fileLocation
        this.version = version
    }

    String getGroup() {
        return group
    }

    String getName() {
        return name
    }

    String getFileLocation() {
        return fileLocation
    }

    String getVersion() {
        return version
    }
}

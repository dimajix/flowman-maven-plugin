/*
 * Copyright 2022 Kaya Kupferschmidt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dimajix.flowman.maven.plugin.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class ExecutionSettings {
    @JsonProperty(value="profiles", required = false)
    private List<String> profiles = Collections.emptyList();

    @JsonProperty(value="environment", required = false)
    private List<String> environment = Collections.emptyList();

    @JsonProperty(value="config", required = false)
    private List<String> config = Collections.emptyList();
}

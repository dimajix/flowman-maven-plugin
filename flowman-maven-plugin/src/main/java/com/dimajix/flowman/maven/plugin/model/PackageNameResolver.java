/*
 * Copyright 2022 The Flowman Authors
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

import java.util.Map;

import com.fasterxml.jackson.databind.util.StdConverter;
import lombok.val;
import lombok.var;

public class PackageNameResolver extends StdConverter<Map<String, Package>, Map<String, Package>> {
    @Override
    public Map<String, Package> convert(Map<String, Package> value) {
        for (var entry : value.entrySet()) {
            val name = entry.getKey();
            val deployment = entry.getValue();
            deployment.setName(name);
        }
        return value;
    }
}

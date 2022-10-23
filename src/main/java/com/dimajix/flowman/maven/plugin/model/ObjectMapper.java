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

import java.io.File;
import java.io.IOException;
import java.net.URL;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.val;


public class ObjectMapper {
    private ObjectMapper() {
    }

    static com.fasterxml.jackson.databind.ObjectMapper getMapper() {
        val mapper = new com.fasterxml.jackson.databind.ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        return mapper;
    }

    static public <T> T read(File file, Class<T> valueType) throws IOException {
        val mapper = getMapper();
        return mapper.readValue(file, valueType);
    }

    static public <T> T read(URL url, Class<T> valueType) throws IOException {
        val mapper = getMapper();
        return mapper.readValue(url, valueType);
    }

    static public <T> T read(String spec, Class<T> valueType) throws IOException {
        val mapper = getMapper();
        return mapper.readValue(spec, valueType);
    }
}

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

package com.dimajix.flowman.maven.plugin.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.val;
import org.apache.commons.lang3.StringUtils;


public class Collections {
    public static <E> List<E> concat(List<E> left, List<E> right) {
        return Stream.concat(left.stream(), right.stream()).collect(Collectors.toList());
    }

    public static <K,V> Map<K,V> concat(Map<K,V> left, Map<K,V> right) {
        val result = new HashMap<K,V>();
        result.putAll(left);
        result.putAll(right);
        return result;
    }

    public static Map<String,String> splitSettings(Collection<String> settings) {
        return settings.stream().map(set -> {
            val parts = StringUtils.split(set, "=", 2);
            if (parts.length > 1) {
                parts[1] = StringUtils.trim(parts[1]);
                parts[1] = StringUtils.unwrap(parts[1], '\"');
                return parts;
            }
            else {
                val res = new String[2];
                res[0] = parts[0];
                res[1] = "";
                return res;
            }
        }).collect(Collectors.toMap(p -> p[0], p -> p[1]));
    }
}

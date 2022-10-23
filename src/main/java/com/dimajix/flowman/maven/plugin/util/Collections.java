package com.dimajix.flowman.maven.plugin.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.val;


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
}

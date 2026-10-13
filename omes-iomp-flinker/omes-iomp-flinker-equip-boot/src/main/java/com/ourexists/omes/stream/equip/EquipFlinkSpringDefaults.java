package com.ourexists.omes.stream.equip;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Flattens Spring {@link ConfigurableEnvironment} into a string map for merging into Flink {@code ParameterTool}.
 * Property source order follows Spring (first source wins for duplicate keys).
 */
final class EquipFlinkSpringDefaults {

    private EquipFlinkSpringDefaults() {
    }

    static Map<String, String> flatten(ConfigurableEnvironment env) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        MutablePropertySources sources = env.getPropertySources();
        for (PropertySource<?> ps : sources) {
            if (!(ps instanceof EnumerablePropertySource<?> eps)) {
                continue;
            }
            for (String name : eps.getPropertyNames()) {
                Object v = eps.getProperty(name);
                if (v != null) {
                    map.putIfAbsent(name, v.toString());
                }
            }
        }
        return map;
    }
}

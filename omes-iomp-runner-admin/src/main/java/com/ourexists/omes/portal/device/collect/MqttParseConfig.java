package com.ourexists.omes.portal.device.collect;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * MQTT 报文解析配置，存在网关 params.parse。
 * grouped 与 TC_MQTT 相同：设备编码 = 根节点编号 + 分组前缀 + 数组内编码，时间在根节点，属性按键名直取。
 */
@Slf4j
public final class MqttParseConfig {

    public static final String MODE_FLAT = "flat";
    public static final String MODE_GROUPED = "grouped";

    private final boolean grouped;
    private final String snField;
    private final String timeField;
    private final String codeField;
    private final List<Group> groups;

    private MqttParseConfig(boolean grouped, String snField, String timeField, String codeField, List<Group> groups) {
        this.grouped = grouped;
        this.snField = snField;
        this.timeField = timeField;
        this.codeField = codeField;
        this.groups = groups;
    }

    public static MqttParseConfig flat() {
        return new MqttParseConfig(false, "SN", "time", "code", List.of());
    }

    /** TC_MQTT 固定规则：dev→前缀 dev，ai→前缀 yw。 */
    public static MqttParseConfig tc() {
        return new MqttParseConfig(true, "SN", "time", "code", List.of(
                new Group("dev", "dev"),
                new Group("ai", "yw")
        ));
    }

    public static MqttParseConfig from(String params) {
        if (!StringUtils.hasText(params)) {
            return flat();
        }
        try {
            JSONObject root = JSON.parseObject(params);
            if (root == null) {
                return flat();
            }
            JSONObject parse = root.getJSONObject("parse");
            if (parse == null) {
                return flat();
            }
            boolean grouped = MODE_GROUPED.equalsIgnoreCase(parse.getString("mode"));
            List<Group> groups = new ArrayList<>();
            JSONArray array = parse.getJSONArray("groups");
            if (array != null) {
                for (int i = 0; i < array.size(); i++) {
                    JSONObject item = array.getJSONObject(i);
                    if (item == null || !StringUtils.hasText(item.getString("array"))) {
                        continue;
                    }
                    String prefix = item.getString("prefix");
                    groups.add(new Group(item.getString("array").trim(), prefix == null ? "" : prefix.trim()));
                }
            }
            return new MqttParseConfig(
                    grouped,
                    textOrDefault(parse.getString("snField"), "SN"),
                    textOrDefault(parse.getString("timeField"), "time"),
                    textOrDefault(parse.getString("codeField"), "code"),
                    List.copyOf(groups));
        } catch (Exception e) {
            log.warn("MQTT parse config invalid, use flat: {}", e.getMessage());
            return flat();
        }
    }

    private static String textOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    public boolean isGrouped() {
        return grouped;
    }

    public String getSnField() {
        return snField;
    }

    public String getTimeField() {
        return timeField;
    }

    public String getCodeField() {
        return codeField;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public static final class Group {
        private final String array;
        private final String prefix;

        public Group(String array, String prefix) {
            this.array = array;
            this.prefix = prefix;
        }

        public String getArray() {
            return array;
        }

        public String getPrefix() {
            return prefix;
        }
    }
}

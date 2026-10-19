package com.ourexists.omes.portal.device.collect;

import com.ourexists.omes.device.core.equip.cache.EquipAlarmRealtime;
import com.ourexists.omes.device.core.equip.cache.EquipAttrRealtime;
import com.ourexists.omes.device.core.equip.cache.EquipControlRealtime;
import com.ourexists.omes.device.core.equip.cache.EquipRealtime;
import com.ourexists.omes.device.core.equip.cache.EquipRealtimeConfig;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 将 DGW 点位编码上的工程值填入设备属性、报警与控制。
 */
@Component
public class DgwEquipDataParser {

    @Autowired
    private AlarmRuleProcessor alarmRuleProcessor;

    public EquipRealtime parse(EquipRealtime source, boolean online, Map<String, Object> values) {
        if (source == null || source.getEquipRealtimeConfig() == null) {
            return null;
        }
        EquipRealtime target = new EquipRealtime();
        BeanUtils.copyProperties(source, target);
        target.setTime(new Date());
        target.setOnlineState(online ? 1 : 0);

        EquipRealtimeConfig config = source.getEquipRealtimeConfig();
        if (StringUtils.hasText(config.getRunMap())) {
            Integer runVal = asInt(values.get(config.getRunMap().trim()));
            target.setRunState(runVal != null && runVal == 1 ? 1 : 0);
        } else if (online) {
            target.setRunState(1);
        }

        if (!CollectionUtils.isEmpty(target.getEquipAttrRealtimes())) {
            for (EquipAttrRealtime attr : target.getEquipAttrRealtimes()) {
                String val = asText(values.get(trim(attr.getMap())));
                if (val != null) {
                    attr.setValue(val);
                }
            }
        }
        if (!CollectionUtils.isEmpty(target.getEquipControlRealtimes())) {
            for (EquipControlRealtime ctrl : target.getEquipControlRealtimes()) {
                String val = asText(values.get(trim(ctrl.getMap())));
                if (val != null) {
                    ctrl.setValue(val);
                }
            }
        }

        int alarm = 0;
        if (!CollectionUtils.isEmpty(config.getAlarms())) {
            List<String> alarms = new ArrayList<>();
            for (EquipAlarmRealtime alarmRealtime : config.getAlarms()) {
                Object raw = values.get(trim(alarmRealtime.getMap()));
                if (alarmRuleProcessor.match(raw, alarmRealtime)) {
                    alarm = 1;
                    alarms.add(alarmRealtime.getText());
                }
            }
            target.setAlarmTexts(alarms);
        }
        target.setAlarmState(alarm);
        return target;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String asText(Object value) {
        return value == null ? null : value.toString();
    }

    private static Integer asInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof Boolean bool) {
            return bool ? 1 : 0;
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

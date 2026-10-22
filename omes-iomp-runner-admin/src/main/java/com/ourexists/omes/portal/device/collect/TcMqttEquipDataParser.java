package com.ourexists.omes.portal.device.collect;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ourexists.omes.device.core.equip.cache.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class TcMqttEquipDataParser implements EquipDataParser {

    @Autowired
    private EquipRealtimeManager equipRealtimeManager;

    @Autowired
    private AlarmRuleProcessor alarmRuleProcessor;

    public List<EquipRealtime> parse(String gwId, String sourceData) {
        return parse(gwId, sourceData, MqttParseConfig.tc());
    }

    public List<EquipRealtime> parse(String gwId, String sourceData, MqttParseConfig config) {
        List<EquipRealtime> targets = new ArrayList<>();
        final JSONObject jsonObject;
        try {
            jsonObject = JSON.parseObject(sourceData);
        } catch (Exception e) {
            log.warn("grouped MQTT equip JSON parse failed gwId={} payloadChars={}", gwId, sourceData.length(), e);
            return targets;
        }
        if (jsonObject == null) {
            log.warn("grouped MQTT equip JSON root null gwId={} payloadChars={}", gwId, sourceData.length());
            return targets;
        }
        if (config == null || config.getGroups().isEmpty()) {
            log.warn("grouped MQTT equip has no groups gwId={}", gwId);
            return targets;
        }
        String sn = jsonObject.getString(config.getSnField());
        String timeStr = jsonObject.getString(config.getTimeField());
        if (!StringUtils.hasText(timeStr)) {
            log.warn("grouped MQTT equip missing time gwId={} sn={} timeField={}", gwId, sn, config.getTimeField());
            return Collections.emptyList();
        }
        final Date time;
        try {
            time = Date.from(OffsetDateTime.parse(timeStr.trim()).toInstant());
        } catch (Exception e) {
            log.warn("grouped MQTT equip time parse failed gwId={} sn={} timeStr={}", gwId, sn, timeStr, e);
            return Collections.emptyList();
        }

        int itemCount = 0;
        for (MqttParseConfig.Group group : config.getGroups()) {
            JSONArray array = jsonObject.getJSONArray(group.getArray());
            if (array == null) {
                continue;
            }
            itemCount += array.size();
            for (JSONObject object : array.toArray(JSONObject.class)) {
                if (object == null) {
                    continue;
                }
                String code = object.getString(config.getCodeField());
                String ssn = sn + group.getPrefix() + code;
                EquipRealtime equipRealtime = equipRealtimeManager.get(ssn);
                if (equipRealtime == null || equipRealtime.getEquipRealtimeConfig() == null
                        || !gwId.equals(equipRealtime.getEquipRealtimeConfig().getGwId())) {
                    continue;
                }
                targets.add(doParse(equipRealtime, object, time));
            }
        }
        if (targets.isEmpty() && itemCount > 0) {
            log.warn(
                    "grouped MQTT equip parse produced 0 rows (cache miss or gw mismatch) gwId={} sn={} itemCount={}",
                    gwId,
                    sn,
                    itemCount);
        }
        return targets;
    }

    public EquipRealtime doParse(EquipRealtime equipRealtime, JSONObject parsedObj, Date time) {
        EquipRealtime target = new EquipRealtime();
        BeanUtils.copyProperties(equipRealtime, target);
        if (equipRealtime.getOnlineState() != 1) {
            target.setOnlineChange(true);
        } else {
            target.setOnlineChange(false);
        }
        target.setTime(time);
        target.setOnlineState(1);
        target.setRunState(1);


        Integer runVal = parsedObj.getInteger(equipRealtime.getEquipRealtimeConfig().getRunMap());
        if (runVal == null || runVal == 1) {
            target.setRunState(1);
        } else {
            target.setRunState(0);
        }

        if (!CollectionUtils.isEmpty(target.getEquipAttrRealtimes())) {
            for (EquipAttrRealtime attr : target.getEquipAttrRealtimes()) {
                attr.setValue(parsedObj.getString(attr.getMap()));
            }
        }

        if (!CollectionUtils.isEmpty(target.getEquipControlRealtimes())) {
            for (EquipControlRealtime ctrl : target.getEquipControlRealtimes()) {
                String val = parsedObj.getString(ctrl.getMap());
                if (val != null) {
                    ctrl.setValue(val);
                }
            }
        }

        int alarm = 0;
        if (!CollectionUtils.isEmpty(equipRealtime.getEquipRealtimeConfig().getAlarms())) {
            List<String> alarms = new ArrayList<>();
            Integer level = -1;
            for (EquipAlarmRealtime alarmRealtime : equipRealtime.getEquipRealtimeConfig().getAlarms()) {
                Object raw = parsedObj.get(alarmRealtime.getMap());
                if (alarmRuleProcessor.match(raw, alarmRealtime)) {
                    alarm = 1;
                    level = alarmRealtime.getLevel() > level ? alarmRealtime.getLevel() : level;
                    alarms.add(alarmRealtime.getText());
                }
            }
            target.setAlarmTexts(alarms);
            target.setAlarmLevel(level);
        }
        target.setAlarmState(alarm);
        return target;
    }
}

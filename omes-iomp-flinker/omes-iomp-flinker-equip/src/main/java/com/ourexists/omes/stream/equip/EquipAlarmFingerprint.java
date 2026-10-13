package com.ourexists.omes.stream.equip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stable alarm fingerprint for change-detect: same text set and level yield the same string regardless of list order;
 * {@code alarmLevel} distinguishes same text at different levels. Key is device {@code selfCode} (sn).
 */
public final class EquipAlarmFingerprint {

    private EquipAlarmFingerprint() {}

    public static String of(String keyPart, Integer level, List<String> alarmTexts) {
        String kp = keyPart == null ? "" : keyPart;
        String levelPart = level == null ? "null" : level.toString();
        if (alarmTexts == null || alarmTexts.isEmpty()) {
            return kp + "|DEFAULT|" + levelPart;
        }
        List<String> sorted = new ArrayList<>();
        for (String s : alarmTexts) {
            if (s != null && !s.isBlank()) {
                sorted.add(s.trim());
            }
        }
        if (sorted.isEmpty()) {
            return kp + "|DEFAULT|" + levelPart;
        }
        Collections.sort(sorted);
        return kp + "|" + levelPart + "|" + String.join("\u0001", sorted);
    }
}

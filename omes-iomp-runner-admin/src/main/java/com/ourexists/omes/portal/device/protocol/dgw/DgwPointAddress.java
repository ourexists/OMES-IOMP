package com.ourexists.omes.portal.device.protocol.dgw;

/**
 * 控制写入地址：设备标识与点位编码用分隔符拼在一起，避免改 {@code ProtocolManager.write} 签名。
 */
public final class DgwPointAddress {

    public static final char SEPARATOR = '\u001f';

    private DgwPointAddress() {
    }

    public static String encode(String deviceKey, String pointCode) {
        return deviceKey + SEPARATOR + pointCode;
    }

    public static String[] decode(String address) {
        if (address == null) {
            return new String[]{null, null};
        }
        int sep = address.indexOf(SEPARATOR);
        if (sep < 0) {
            return new String[]{null, address};
        }
        return new String[]{address.substring(0, sep), address.substring(sep + 1)};
    }
}

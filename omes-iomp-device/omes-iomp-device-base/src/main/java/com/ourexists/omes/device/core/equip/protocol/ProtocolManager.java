package com.ourexists.omes.device.core.equip.protocol;

public interface ProtocolManager {

    boolean start(ProtocolConnect connect);

    boolean stop(String connectId);

    void stopAll();

    String protocol();

    /**
     * 本进程里该连接的连通情况：{@code up} 已连通，{@code down} 已启动但当前不通，{@code pending} 已启动但还没有一次通信结果。
     * 未拉起时返回 null。
     */
    default String linkState(String connectId) {
        return null;
    }

    /** 最近一次连通失败原因；没有则返回 null。 */
    default String linkMessage(String connectId) {
        return null;
    }

    /**
     * 向设备写入控制值。
     *
     * @param connectId 网关连接 ID
     * @param address   PLC/设备 可写地址
     * @param value     写入值
     * @return 是否写入成功
     */
    default boolean write(String connectId, String address, Object value) {
        throw new UnsupportedOperationException(protocol() + " does not support write");
    }
}

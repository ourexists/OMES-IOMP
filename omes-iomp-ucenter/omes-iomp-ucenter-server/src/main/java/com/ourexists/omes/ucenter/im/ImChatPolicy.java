/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */
package com.ourexists.omes.ucenter.im;

/**
 * Who may appear in a tenant service chat. A user only talks to that tenant's service account.
 */
public final class ImChatPolicy {

    public static final String SERVICE_PREFIX = "cs_";

    public static final String PEER_NAME = "客服";

    public static final String DESK_URL = "/view/im_desk";

    public static final int SYNC_OK = 1;

    public static final int SYNC_FAIL = 2;

    public static final int SYNC_STOPPED = 3;

    private ImChatPolicy() {
    }

    public static String serviceUserId(String tenantId) {
        return SERVICE_PREFIX + tenantId;
    }

    /**
     * @return the human account id when the pair is exactly this tenant's service account and someone else
     */
    public static String humanPeer(String sendId, String recvId, String tenantId) {
        if (sendId == null || recvId == null || tenantId == null) {
            return null;
        }
        String serviceId = serviceUserId(tenantId);
        if (serviceId.equals(sendId) && !serviceId.equals(recvId)) {
            return recvId;
        }
        if (serviceId.equals(recvId) && !serviceId.equals(sendId)) {
            return sendId;
        }
        return null;
    }
}

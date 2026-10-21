/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */
package com.ourexists.omes.ucenter.im;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ourexists.era.framework.core.exceptions.BusinessException;
import com.ourexists.era.framework.core.user.UserContext;
import com.ourexists.era.framework.core.user.UserInfo;
import com.ourexists.era.oauth2.core.OAuth2Role;
import com.ourexists.omes.ucenter.account.pojo.Account;
import com.ourexists.omes.ucenter.account.service.AccountService;
import com.ourexists.omes.ucenter.enums.AccStatusEnum;
import com.ourexists.omes.ucenter.im.mapper.ImAccountMapper;
import com.ourexists.omes.ucenter.im.mapper.ImServiceAccountMapper;
import com.ourexists.omes.ucenter.im.pojo.ImAccount;
import com.ourexists.omes.ucenter.im.pojo.ImServiceAccount;
import com.ourexists.omes.ucenter.permission.pojo.Permission;
import com.ourexists.omes.ucenter.permission.service.PermissionService;
import com.ourexists.omes.ucenter.tenant.pojo.TenantAcc;
import com.ourexists.omes.ucenter.tenant.service.TenantAccService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ImChatService {

    private static final int MAX_TEXT = 2000;

    @Autowired
    private WuKongImClient wuKongImClient;

    @Autowired
    private WuKongImProperties properties;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TenantAccService tenantAccService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ImAccountMapper imAccountMapper;

    @Autowired
    private ImServiceAccountMapper imServiceAccountMapper;

    public ImSessionVo openSession() {
        Account me = requireCurrentAccount();
        String tenantId = tenantId();
        requireMember(me.getId(), tenantId);
        ensurePerson(me);
        ensureService(tenantId);
        wuKongImClient.connect(me.getId());
        ImSessionVo vo = new ImSessionVo();
        vo.setPeerName(ImChatPolicy.PEER_NAME);
        return vo;
    }

    public void closeSession() {
        Account me = requireCurrentAccount();
        wuKongImClient.disconnect(me.getId());
    }

    public List<ImMessageVo> messages() {
        Account me = requireCurrentAccount();
        String tenantId = tenantId();
        requireMember(me.getId(), tenantId);
        return history(me.getId(), ImChatPolicy.serviceUserId(tenantId));
    }

    public void send(String text) {
        Account me = requireCurrentAccount();
        String tenantId = tenantId();
        requireMember(me.getId(), tenantId);
        String body = requireText(text);
        ensurePerson(me);
        ensureService(tenantId);
        wuKongImClient.sendText(me.getId(), ImChatPolicy.serviceUserId(tenantId), display(me), body);
    }

    public List<ImDeskSessionVo> deskSessions() {
        requireDesk();
        String tenantId = tenantId();
        String serviceId = ensureService(tenantId);
        Map<String, WuKongImClient.ImHit> latest = new LinkedHashMap<>();
        for (WuKongImClient.ImHit hit : wuKongImClient.conversations(serviceId)) {
            WuKongImClient.ImHit prev = latest.get(hit.userId());
            if (prev == null || hit.sendTime() >= prev.sendTime()) {
                latest.put(hit.userId(), hit);
            }
        }
        Map<String, String> names = names(latest.keySet());
        List<ImDeskSessionVo> sessions = new ArrayList<>();
        for (WuKongImClient.ImHit hit : latest.values()) {
            if (!member(hit.userId(), tenantId)) {
                continue;
            }
            ImDeskSessionVo vo = new ImDeskSessionVo();
            vo.setUserAccId(hit.userId());
            vo.setNickName(names.getOrDefault(hit.userId(), hit.userId()));
            vo.setPreview(hit.preview());
            vo.setSendTime(hit.sendTime());
            sessions.add(vo);
        }
        sessions.sort(Comparator.comparingLong(ImDeskSessionVo::getSendTime).reversed());
        return sessions;
    }

    public List<ImMessageVo> deskMessages(String userAccId) {
        requireDesk();
        String tenantId = tenantId();
        requirePeer(userAccId, tenantId);
        return history(userAccId, ImChatPolicy.serviceUserId(tenantId));
    }

    public void deskSend(String userAccId, String text) {
        requireDesk();
        String tenantId = tenantId();
        Account peer = requirePeer(userAccId, tenantId);
        String body = requireText(text);
        String serviceId = ensureService(tenantId);
        ensurePerson(peer);
        wuKongImClient.sendText(serviceId, peer.getId(), ImChatPolicy.PEER_NAME, body);
    }

    public void onAccountClosed(String accId) {
        if (!StringUtils.hasText(accId) || !properties.isEnabled()) {
            return;
        }
        try {
            wuKongImClient.drop(accId);
            markUser(accId, ImChatPolicy.SYNC_STOPPED, null);
        } catch (Exception ex) {
            log.warn("im kick failed acc={}", accId);
        }
    }

    private List<ImMessageVo> history(String userAccId, String serviceId) {
        List<WuKongImClient.ImRaw> raw = new ArrayList<>(wuKongImClient.searchSingle(userAccId, serviceId));
        raw.sort(Comparator.comparingLong(WuKongImClient.ImRaw::sendTime));
        Map<String, String> names = names(List.of(userAccId));
        List<ImMessageVo> messages = new ArrayList<>();
        for (WuKongImClient.ImRaw item : raw) {
            if (!StringUtils.hasText(item.text())) {
                continue;
            }
            boolean fromUser = userAccId.equals(item.sendId());
            boolean fromService = serviceId.equals(item.sendId());
            if (!fromUser && !fromService) {
                continue;
            }
            if (StringUtils.hasText(item.recvId())
                    && !userAccId.equals(item.recvId())
                    && !serviceId.equals(item.recvId())) {
                continue;
            }
            ImMessageVo vo = new ImMessageVo();
            vo.setSendId(item.sendId());
            vo.setFromService(fromService);
            vo.setSenderName(fromService ? ImChatPolicy.PEER_NAME : names.getOrDefault(userAccId, userAccId));
            vo.setText(item.text());
            vo.setSendTime(item.sendTime());
            messages.add(vo);
        }
        return messages;
    }

    private void ensurePerson(Account account) {
        try {
            wuKongImClient.ensureUser(account.getId(), display(account));
            markUser(account.getId(), ImChatPolicy.SYNC_OK, null);
        } catch (RuntimeException ex) {
            markUser(account.getId(), ImChatPolicy.SYNC_FAIL, ex.getMessage());
            throw ex;
        }
    }

    private String ensureService(String tenantId) {
        String serviceId = ImChatPolicy.serviceUserId(tenantId);
        try {
            wuKongImClient.ensureUser(serviceId, ImChatPolicy.PEER_NAME);
            markService(tenantId, serviceId, ImChatPolicy.SYNC_OK, null);
            return serviceId;
        } catch (RuntimeException ex) {
            markService(tenantId, serviceId, ImChatPolicy.SYNC_FAIL, ex.getMessage());
            throw ex;
        }
    }

    private Account requireCurrentAccount() {
        UserInfo user = UserContext.getUser();
        if (user == null || !StringUtils.hasText(user.getId())) {
            throw new BusinessException("未登录");
        }
        Account account = accountService.getById(user.getId());
        if (account == null || !AccStatusEnum.COMMON.getCode().equals(account.getStatus())) {
            throw new BusinessException("账号不可用");
        }
        return account;
    }

    private void requireDesk() {
        Account me = requireCurrentAccount();
        String platform = UserContext.getPlatForm();
        List<Permission> held;
        if (UserContext.getTenant() != null
                && OAuth2Role.ADMIN.name().equals(UserContext.getTenant().getRole())) {
            // Same source as the admin menu: the tenant set, which is every menu on the system tenant.
            held = permissionService.selectPermissionWhichTenantHold(UserContext.getTenant().getTenantId(), platform);
        } else {
            held = permissionService.selectPermissionWhichAccHold(me.getId(), platform);
        }
        boolean allowed = held != null && held.stream().anyMatch(item -> ImChatPolicy.DESK_URL.equals(item.getUrl()));
        if (!allowed) {
            throw new BusinessException("没有客服工作台权限");
        }
    }

    private Account requirePeer(String userAccId, String tenantId) {
        if (!StringUtils.hasText(userAccId)) {
            throw new BusinessException("请选择会话");
        }
        Account account = accountService.getById(userAccId);
        if (account == null || !AccStatusEnum.COMMON.getCode().equals(account.getStatus()) || !member(userAccId, tenantId)) {
            throw new BusinessException("对方不在当前租户");
        }
        return account;
    }

    private void requireMember(String accId, String tenantId) {
        if (!member(accId, tenantId)) {
            throw new BusinessException("不在当前租户");
        }
    }

    private boolean member(String accId, String tenantId) {
        return tenantAccService.count(new LambdaQueryWrapper<TenantAcc>()
                .eq(TenantAcc::getAccId, accId)
                .eq(TenantAcc::getTenantId, tenantId)) > 0;
    }

    private String tenantId() {
        if (UserContext.getTenant() == null || !StringUtils.hasText(UserContext.getTenant().getTenantId())) {
            throw new BusinessException("未选择租户");
        }
        return UserContext.getTenant().getTenantId();
    }

    private String requireText(String text) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("请输入消息");
        }
        String body = text.trim();
        if (body.length() > MAX_TEXT) {
            throw new BusinessException("消息过长");
        }
        return body;
    }

    private Map<String, String> names(Iterable<String> ids) {
        List<String> idList = new ArrayList<>();
        ids.forEach(idList::add);
        Map<String, String> names = new HashMap<>();
        if (idList.isEmpty()) {
            return names;
        }
        for (Account account : accountService.listByIds(idList)) {
            names.put(account.getId(), display(account));
        }
        return names;
    }

    private String display(Account account) {
        if (StringUtils.hasText(account.getNickName())) {
            return account.getNickName();
        }
        if (StringUtils.hasText(account.getAccName())) {
            return account.getAccName();
        }
        return account.getId();
    }

    private void markUser(String accId, int status, String error) {
        try {
            ImAccount row = imAccountMapper.selectOne(new LambdaQueryWrapper<ImAccount>()
                    .eq(ImAccount::getAccId, accId)
                    .last("LIMIT 1"));
            Date now = new Date();
            if (row == null) {
                row = new ImAccount()
                        .setAccId(accId)
                        .setImUserId(accId)
                        .setSyncStatus(status)
                        .setLastError(clipError(error))
                        .setLastSyncTime(now);
                row.setId(IdWorker.getIdStr());
                row.setRevision(0);
                imAccountMapper.insert(row);
                return;
            }
            row.setSyncStatus(status);
            row.setLastError(clipError(error));
            row.setLastSyncTime(now);
            imAccountMapper.updateById(row);
        } catch (Exception ex) {
            log.warn("im account row failed acc={}", accId);
        }
    }

    private void markService(String tenantId, String serviceId, int status, String error) {
        try {
            ImServiceAccount row = imServiceAccountMapper.selectOne(new LambdaQueryWrapper<ImServiceAccount>()
                    .eq(ImServiceAccount::getTenantId, tenantId)
                    .last("LIMIT 1"));
            Date now = new Date();
            if (row == null) {
                row = new ImServiceAccount()
                        .setTenantId(tenantId)
                        .setImUserId(serviceId)
                        .setSyncStatus(status)
                        .setLastError(clipError(error))
                        .setLastSyncTime(now);
                row.setId(IdWorker.getIdStr());
                row.setRevision(0);
                imServiceAccountMapper.insert(row);
                return;
            }
            row.setSyncStatus(status);
            row.setImUserId(serviceId);
            row.setLastError(clipError(error));
            row.setLastSyncTime(now);
            imServiceAccountMapper.updateById(row);
        } catch (Exception ex) {
            log.warn("im service row failed tenant={}", tenantId);
        }
    }

    private String clipError(String error) {
        if (!StringUtils.hasText(error)) {
            return null;
        }
        return error.length() <= 500 ? error : error.substring(0, 500);
    }
}

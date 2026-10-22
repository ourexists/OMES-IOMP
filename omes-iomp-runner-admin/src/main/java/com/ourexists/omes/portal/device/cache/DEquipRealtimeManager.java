package com.ourexists.omes.portal.device.cache;

import com.ourexists.era.framework.core.exceptions.BusinessException;
import com.ourexists.era.framework.core.model.vo.JsonResponseEntity;
import com.ourexists.era.framework.core.user.UserContext;
import com.ourexists.omes.device.core.equip.cache.*;
import com.ourexists.omes.device.feign.EquipFeign;
import com.ourexists.omes.device.model.EquipDto;
import com.ourexists.omes.device.model.EquipPageQuery;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DEquipRealtimeManager implements EquipRealtimeManager {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private EquipFeign equipFeign;

    @Autowired
    private RedisCacheValueConverter cacheValueConverter;

    @Autowired
    private RedissonClient redissonClient;

    private static final String EQUIP_DATA_SYNC_LOCK_KEY = "omes:equip_rt:data_sync";

    @Value("${omes.device.equip-data-sync-lock-wait-seconds:30}")
    private long equipDataSyncLockWaitSeconds;

    private static String equipRedisCacheName(String tenantId) {
        return "{" + tenantId + "}equip_realtime_" + tenantId;
    }

    private static final int RELOAD_PAGE_SIZE = 500;

    private static String idToSnIndexKey(String tenantId) {
        return "{" + tenantId + "}omes:equip_rt:id2sn";
    }

    private static String gwToSnSetKey(String tenantId, String gwId) {
        return "{" + tenantId + "}omes:equip_rt:gw2sn:" + gwId;
    }

    private static String stateOnlineZsetKey(String tenantId) {
        return "{" + tenantId + "}omes:equip_rt:z_online";
    }

    private static String stateRunZsetKey(String tenantId) {
        return "{" + tenantId + "}omes:equip_rt:z_run";
    }

    private static String stateAlarmZsetKey(String tenantId) {
        return "{" + tenantId + "}omes:equip_rt:z_alarm";
    }

    private static int stateFlag01(Integer state) {
        return state != null && state == 1 ? 1 : 0;
    }

    private static long millisOrNow(Date d) {
        return d != null ? d.getTime() : System.currentTimeMillis();
    }

    private Cache tenantCache() {
        String tenantId = UserContext.getTenant().getTenantId();
        Cache springCache = cacheManager.getCache(equipRedisCacheName(tenantId));
        if (springCache == null) {
            throw new BusinessException("缓存未初始化: " + equipRedisCacheName(tenantId));
        }
        return springCache;
    }

    private static String equipCacheRedisKey(String tenantId, String selfCode) {
        return equipRedisCacheName(tenantId) + "::" + selfCode;
    }

    private static String resolveGwId(EquipRealtime rt) {
        if (rt == null || rt.getEquipRealtimeConfig() == null) {
            return null;
        }
        String gwId = rt.getEquipRealtimeConfig().getGwId();
        return StringUtils.hasText(gwId) ? gwId : null;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    @Override
    public void addOrUpdate(EquipRealtime equipRealtime) {
        withEquipDataSyncMutation(() -> {
            String tenantId = UserContext.getTenant().getTenantId();
            String sn = equipRealtime.getSelfCode();
            Cache.ValueWrapper oldW = tenantCache().get(sn);
            EquipRealtime old = oldW == null ? null : cacheValueConverter.convert(oldW.get(), EquipRealtime.class);
            String oldGw = resolveGwId(old);
            String newGw = resolveGwId(equipRealtime);
            luaUpsertEquip(tenantId, sn, equipRealtime, oldGw, newGw);
        });
    }

    @Override
    public void remove(String sn) {
        withEquipDataSyncMutation(() -> {
            Cache.ValueWrapper w = tenantCache().get(sn);
            EquipRealtime er = w == null ? null : cacheValueConverter.convert(w.get(), EquipRealtime.class);
            String tenantId = UserContext.getTenant().getTenantId();
            if (er == null) {
                tenantCache().evict(sn);
                return;
            }
            String idHash = idToSnIndexKey(tenantId);
            String mainKey = equipCacheRedisKey(tenantId, sn);
            String gwId = resolveGwId(er);
            boolean sremGw = StringUtils.hasText(gwId);
            String gwKey = sremGw ? gwToSnSetKey(tenantId, gwId) : idHash;
            EquipRealtimeRedisLua.executeRemove(
                    redisTemplate,
                    Arrays.asList(
                            mainKey,
                            idHash,
                            gwKey,
                            stateOnlineZsetKey(tenantId),
                            stateRunZsetKey(tenantId),
                            stateAlarmZsetKey(tenantId)),
                    er.getId(),
                    sn,
                    sremGw);
        });
    }

    @Override
    public EquipRealtime get(String sn) {
        Cache.ValueWrapper valueWrapper = tenantCache().get(sn);
        return valueWrapper == null ? null : cacheValueConverter.convert(valueWrapper.get(), EquipRealtime.class);
    }

    @Override
    public EquipRealtime getById(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        String tenantId = UserContext.getTenant().getTenantId();
        Object sn = stringRedisTemplate.opsForHash().get(idToSnIndexKey(tenantId), id);
        if (!(sn instanceof String selfCode) || !StringUtils.hasText(selfCode)) {
            return null;
        }
        return get(selfCode);
    }

    @Override
    public List<EquipRealtime> listByGwId(String gwId) {
        if (!StringUtils.hasText(gwId)) {
            return Collections.emptyList();
        }
        String tenantId = UserContext.getTenant().getTenantId();
        Set<String> sns;
        try {
            sns = stringRedisTemplate.opsForSet().members(gwToSnSetKey(tenantId, gwId));
        } catch (Exception ex) {
            log.error("listByGwId SMEMBERS failed: tenantId={} gwId={}", tenantId, gwId, ex);
            return Collections.emptyList();
        }
        if (CollectionUtils.isEmpty(sns)) {
            return Collections.emptyList();
        }
        List<EquipRealtime> list = new ArrayList<>(sns.size());
        for (String sn : sns) {
            if (!StringUtils.hasText(sn)) {
                continue;
            }
            EquipRealtime rt = get(sn);
            if (rt != null) {
                list.add(rt);
            }
        }
        return list;
    }

    @Override
    public void reload() {
        RLock lock = redissonClient.getLock(EQUIP_DATA_SYNC_LOCK_KEY);
        boolean locked;
        try {
            locked = lock.tryLock(0, -1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("reload equip realtime interrupted while acquiring Redisson lock");
            return;
        }
        if (!locked) {
            log.info("reload equip realtime skipped: data sync lock busy ({})", EQUIP_DATA_SYNC_LOCK_KEY);
            return;
        }
        try {
            UserContext.defaultTenant();
            try {
                Set<String> tenantsIdIndexReset = new HashSet<>();
                int pageNum = 1;
                while (true) {
                    EquipPageQuery query = new EquipPageQuery();
                    query.setRequirePage(true);
                    query.setPage(pageNum);
                    query.setPageSize(RELOAD_PAGE_SIZE);
                    query.setQueryConfig(true);
                    JsonResponseEntity<List<EquipDto>> response = equipFeign.selectByPage(query);
                    List<EquipDto> equipDtos = response == null ? null : response.getData();
                    if (CollectionUtils.isEmpty(equipDtos)) {
                        break;
                    }
                    putReloadBatch(equipDtos, tenantsIdIndexReset);
                    if (equipDtos.size() < RELOAD_PAGE_SIZE) {
                        break;
                    }
                    pageNum++;
                }
            } catch (Exception e) {
                log.error("reload equip realtime cache failed: {}", e.getMessage(), e);
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void putReloadBatch(List<EquipDto> equipDtos, Set<String> tenantsIdIndexReset) {
        Map<String, Map<String, EquipRealtime>> equipRealtimeMap = new HashMap<>();
        for (EquipDto equipDto : equipDtos) {
            String tid = equipDto.getTenantId();
            if (tid != null && tenantsIdIndexReset.add(tid)) {
                try {
                    EquipRealtimeRedisLua.executeClearTenant(
                            redisTemplate,
                            equipRedisCacheName(tid) + "::*",
                            idToSnIndexKey(tid),
                            "{" + tid + "}omes:equip_rt:gw2sn:*",
                            stateOnlineZsetKey(tid),
                            stateRunZsetKey(tid),
                            stateAlarmZsetKey(tid));
                } catch (Exception ex) {
                    log.error("reload: atomic clear tenant redis failed tid={}; stale equip/index may remain", tid, ex);
                }
            }
            if (equipDto.getTenantId() == null) {
                log.warn("reload: skip equip with null tenantId selfCode={}", equipDto.getSelfCode());
                continue;
            }
            Map<String, EquipRealtime> r = equipRealtimeMap.computeIfAbsent(equipDto.getTenantId(), k -> new HashMap<>());
            EquipRealtime equipRealtime = new EquipRealtime();
            BeanUtils.copyProperties(equipDto, equipRealtime);

            if (equipDto.getConfig() != null && equipDto.getConfig().getConfig() != null) {
                EquipRealtimeConfig equipRealtimeConfig = new EquipRealtimeConfig();
                BeanUtils.copyProperties(equipDto.getConfig().getConfig(), equipRealtimeConfig);
                if (!CollectionUtils.isEmpty(equipDto.getConfig().getConfig().getAttrs())) {
                    List<EquipAttrRealtime> attrs = new ArrayList<>();
                    equipDto.getConfig().getConfig().getAttrs().forEach(attr -> {
                        EquipAttrRealtime equipAttrRealtime = new EquipAttrRealtime();
                        BeanUtils.copyProperties(attr, equipAttrRealtime);
                        attrs.add(equipAttrRealtime);
                    });
                    equipRealtimeConfig.setAttrs(attrs);
                }
                if (!CollectionUtils.isEmpty(equipDto.getConfig().getConfig().getAlarms())) {
                    List<EquipAlarmRealtime> alarms = new ArrayList<>();
                    equipDto.getConfig().getConfig().getAlarms().forEach(alarm -> {
                        EquipAlarmRealtime equipAlarmRealtime = new EquipAlarmRealtime();
                        BeanUtils.copyProperties(alarm, equipAlarmRealtime);
                        alarms.add(equipAlarmRealtime);
                    });
                    equipRealtimeConfig.setAlarms(alarms);
                }
                if (!CollectionUtils.isEmpty(equipDto.getConfig().getConfig().getControls())) {
                    List<EquipControlRealtime> controls = new ArrayList<>();
                    equipDto.getConfig().getConfig().getControls().forEach(ctrl -> {
                        EquipControlRealtime equipControlRealtime = new EquipControlRealtime();
                        BeanUtils.copyProperties(ctrl, equipControlRealtime);
                        controls.add(equipControlRealtime);
                    });
                    equipRealtimeConfig.setControls(controls);
                }
                equipRealtime.setEquipRealtimeConfig(equipRealtimeConfig);
                equipRealtime.setEquipAttrRealtimes(equipRealtimeConfig.getAttrs());
                equipRealtime.setEquipControlRealtimes(equipRealtimeConfig.getControls());
                Date currentDate = new Date();
                equipRealtime.setAlarmChangeTime(currentDate);
                equipRealtime.setRunChangeTime(currentDate);
                equipRealtime.setOnlineChangeTime(currentDate);
            }
            // 服务启动 reload：尚无实时数据，运行/报警均为未知
            equipRealtime.setRunState(-1);
            equipRealtime.setAlarmState(-1);
            r.put(equipDto.getSelfCode(), equipRealtime);
        }
        for (Map.Entry<String, Map<String, EquipRealtime>> entry : equipRealtimeMap.entrySet()) {
            String tenantKey = entry.getKey();
            for (Map.Entry<String, EquipRealtime> snEntry : entry.getValue().entrySet()) {
                String sn = snEntry.getKey();
                EquipRealtime rt = snEntry.getValue();
                luaReloadUpsertEquip(tenantKey, sn, rt);
            }
        }
    }

    private void luaUpsertEquip(String tenantId, String sn, EquipRealtime rt, String oldGw, String newGw) {
        boolean sremOldGw = StringUtils.hasText(oldGw) && (!Objects.equals(oldGw, newGw) || !StringUtils.hasText(newGw));
        boolean saddNewGw = StringUtils.hasText(newGw);
        String idHash = idToSnIndexKey(tenantId);
        String kMain = equipCacheRedisKey(tenantId, sn);
        String kOldGw = sremOldGw ? gwToSnSetKey(tenantId, oldGw) : idHash;
        String kNewGw = saddNewGw ? gwToSnSetKey(tenantId, newGw) : idHash;
        String kOnline = stateOnlineZsetKey(tenantId);
        String kRun = stateRunZsetKey(tenantId);
        String kAlarm = stateAlarmZsetKey(tenantId);
        EquipRealtimeRedisLua.executeUpsert(
                redisTemplate,
                Arrays.asList(kMain, idHash, kOldGw, kNewGw, kOnline, kRun, kAlarm),
                rt,
                rt.getId(),
                sn,
                sremOldGw,
                saddNewGw,
                stateFlag01(rt.getOnlineState()),
                millisOrNow(rt.getOnlineChangeTime()),
                stateFlag01(rt.getRunState()),
                millisOrNow(rt.getRunChangeTime()),
                stateFlag01(rt.getAlarmState()),
                millisOrNow(rt.getAlarmChangeTime()));
    }

    private void luaReloadUpsertEquip(String tenantId, String sn, EquipRealtime rt) {
        boolean saddNewGw = StringUtils.hasText(resolveGwId(rt));
        String idHash = idToSnIndexKey(tenantId);
        String kMain = equipCacheRedisKey(tenantId, sn);
        String newGw = resolveGwId(rt);
        String kNewGw = saddNewGw ? gwToSnSetKey(tenantId, newGw) : idHash;
        String kOnline = stateOnlineZsetKey(tenantId);
        String kRun = stateRunZsetKey(tenantId);
        String kAlarm = stateAlarmZsetKey(tenantId);
        EquipRealtimeRedisLua.executeUpsert(
                redisTemplate,
                Arrays.asList(kMain, idHash, idHash, kNewGw, kOnline, kRun, kAlarm),
                rt,
                rt.getId(),
                sn,
                false,
                saddNewGw,
                stateFlag01(rt.getOnlineState()),
                millisOrNow(rt.getOnlineChangeTime()),
                stateFlag01(rt.getRunState()),
                millisOrNow(rt.getRunChangeTime()),
                stateFlag01(rt.getAlarmState()),
                millisOrNow(rt.getAlarmChangeTime()));
    }

    private void withEquipDataSyncMutation(Runnable action) {
        RLock lock = redissonClient.getLock(EQUIP_DATA_SYNC_LOCK_KEY);
        try {
            if (!lock.tryLock(equipDataSyncLockWaitSeconds, -1, TimeUnit.SECONDS)) {
                throw new BusinessException("设备实时缓存繁忙，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("操作被中断");
        }
        try {
            action.run();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}

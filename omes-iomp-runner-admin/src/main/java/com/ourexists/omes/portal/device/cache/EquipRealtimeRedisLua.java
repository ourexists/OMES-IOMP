package com.ourexists.omes.portal.device.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.List;

/**
 * 设备实时缓存与 id / gw 索引 / 在线·运行·报警三态 ZSET 索引的一次性原子写入（Redis Lua）。
 * <p>Redis Cluster 要求脚本内访问的 key 落在同一 slot；调用方须使用同一 {@code {tenantId}} hash tag
 * 命名主缓存、id 索引 Hash、gw Set、三态 ZSET（与 {@link org.springframework.data.redis.cache.RedisCache} 的 cache 名一致）。
 */
final class EquipRealtimeRedisLua {

    private static final DefaultRedisScript<Long> UPSERT_SCRIPT = new DefaultRedisScript<>();
    private static final DefaultRedisScript<Long> REMOVE_SCRIPT = new DefaultRedisScript<>();
    private static final DefaultRedisScript<Long> CLEAR_TENANT_SCRIPT = new DefaultRedisScript<>();

    static {
        UPSERT_SCRIPT.setScriptText("""
                redis.call('SET', KEYS[1], ARGV[1])
                if ARGV[2] ~= '' then
                  redis.call('HSET', KEYS[2], ARGV[2], ARGV[3])
                end
                if ARGV[4] == '1' then
                  redis.call('SREM', KEYS[3], ARGV[3])
                end
                if ARGV[5] == '1' then
                  redis.call('SADD', KEYS[4], ARGV[3])
                end
                if ARGV[6] == '1' then
                  redis.call('ZADD', KEYS[5], tonumber(ARGV[7]), ARGV[3])
                else
                  redis.call('ZREM', KEYS[5], ARGV[3])
                end
                if ARGV[8] == '1' then
                  redis.call('ZADD', KEYS[6], tonumber(ARGV[9]), ARGV[3])
                else
                  redis.call('ZREM', KEYS[6], ARGV[3])
                end
                if ARGV[10] == '1' then
                  redis.call('ZADD', KEYS[7], tonumber(ARGV[11]), ARGV[3])
                else
                  redis.call('ZREM', KEYS[7], ARGV[3])
                end
                return 1
                """);
        UPSERT_SCRIPT.setResultType(Long.class);

        REMOVE_SCRIPT.setScriptText("""
                redis.call('DEL', KEYS[1])
                if ARGV[1] ~= '' then
                  redis.call('HDEL', KEYS[2], ARGV[1])
                end
                if ARGV[3] == '1' then
                  redis.call('SREM', KEYS[3], ARGV[2])
                end
                redis.call('ZREM', KEYS[4], ARGV[2])
                redis.call('ZREM', KEYS[5], ARGV[2])
                redis.call('ZREM', KEYS[6], ARGV[2])
                return 1
                """);
        REMOVE_SCRIPT.setResultType(Long.class);

        CLEAR_TENANT_SCRIPT.setScriptText("""
                local function del_by_pattern(pattern)
                  local cursor = '0'
                  repeat
                    local reply = redis.call('SCAN', cursor, 'MATCH', pattern, 'COUNT', 500)
                    cursor = reply[1]
                    local ks = reply[2]
                    for i = 1, #ks do
                      redis.call('DEL', ks[i])
                    end
                  until cursor == '0'
                end
                del_by_pattern(ARGV[1])
                redis.call('DEL', KEYS[1])
                del_by_pattern(ARGV[2])
                redis.call('DEL', ARGV[3], ARGV[4], ARGV[5])
                return 1
                """);
        CLEAR_TENANT_SCRIPT.setResultType(Long.class);
    }

    private EquipRealtimeRedisLua() {
    }

    /** 设备实时主缓存不过期，仅显式 remove / clear 时删除。 */
    static void executeUpsert(
            RedisTemplate<String, Object> redisTemplate,
            List<String> keys,
            Object equipSerializedPayload,
            String equipId,
            String selfCode,
            boolean sremOldGw,
            boolean saddNewGw,
            int onlineInIndex,
            long onlineScoreMillis,
            int runInIndex,
            long runScoreMillis,
            int alarmInIndex,
            long alarmScoreMillis) {
        redisTemplate.execute(
                UPSERT_SCRIPT,
                keys,
                equipSerializedPayload,
                equipId == null ? "" : equipId,
                selfCode,
                sremOldGw ? "1" : "0",
                saddNewGw ? "1" : "0",
                onlineInIndex == 1 ? "1" : "0",
                Long.toString(onlineScoreMillis),
                runInIndex == 1 ? "1" : "0",
                Long.toString(runScoreMillis),
                alarmInIndex == 1 ? "1" : "0",
                Long.toString(alarmScoreMillis));
    }

    static void executeRemove(
            RedisTemplate<String, Object> redisTemplate,
            List<String> keys,
            String equipId,
            String selfCode,
            boolean sremGw) {
        redisTemplate.execute(
                REMOVE_SCRIPT,
                keys,
                equipId == null ? "" : equipId,
                selfCode,
                sremGw ? "1" : "0");
    }

    /**
     * 原子清空当前租户：Spring 设备主缓存键、id→sn Hash、全部 gw→sn Set、三态 ZSET 索引。
     * {@code KEYS[1]} 须为 id→sn 的完整 key，以便 Redis Cluster 将脚本路由到租户所在 slot；脚本内 SCAN 仅在该节点执行即可覆盖同 tag 全部 key。
     */
    static void executeClearTenant(
            RedisTemplate<String, Object> redisTemplate,
            String equipRealtimeKeyPattern,
            String idToSnHashKey,
            String gwToSnKeyPattern,
            String onlineZsetKey,
            String runZsetKey,
            String alarmZsetKey) {
        redisTemplate.execute(
                CLEAR_TENANT_SCRIPT,
                Collections.singletonList(idToSnHashKey),
                equipRealtimeKeyPattern,
                gwToSnKeyPattern,
                onlineZsetKey,
                runZsetKey,
                alarmZsetKey);
    }
}

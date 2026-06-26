-- KEYS[1] = seckill:stock:{activityId}
-- KEYS[2] = seckill:users:{activityId}
-- ARGV[1] = userId

local has = redis.call('SISMEMBER', KEYS[2], ARGV[1])
if has == 1 then
    return -1
end

local stock = redis.call('GET', KEYS[1])
if not stock or tonumber(stock) <= 0 then
    return -2
end

redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
return 1

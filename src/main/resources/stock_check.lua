-- 库存检查和预减的Lua脚本
-- KEYS[1]: 库存key
-- ARGV[1]: 要扣减的数量

local stock = redis.call('get', KEYS[1])
if not stock or tonumber(stock) < tonumber(ARGV[1]) then
    -- 库存不足
    return -1
end

-- 库存充足，减库存
redis.call('decrby', KEYS[1], ARGV[1])
-- 返回剩余库存
return tonumber(stock) - tonumber(ARGV[1])
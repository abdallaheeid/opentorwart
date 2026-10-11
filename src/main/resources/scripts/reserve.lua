-- KEYS[1] = budget key, ARGV[1] = estimate, ARGV[2] = limit, ARGV[3] = TTL seconds
local current  = tonumber(redis.call('GET', KEYS[1]) or '0')
local estimate = tonumber(ARGV[1])
local limit    = tonumber(ARGV[2])

if current + estimate > limit then
	return -1
end

local updated = redis.call('INCRBY', KEYS[1], estimate)
redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]))
return updated
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
	redis.call('INCR', KEYS[1])
	redis.call('SREM', KEYS[2], ARGV[1])
end

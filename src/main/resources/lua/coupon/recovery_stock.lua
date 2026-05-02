redis.call('DEL', KEYS[1], KEYS[2])
redis.call('SET', KEYS[1], ARGV[1])

for i = 2, #ARGV do
	redis.call('SADD', KEYS[2], ARGV[i])
end

return 1

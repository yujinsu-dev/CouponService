local ALREADY_ISSUED = "ALREADY_ISSUED"
local SOLD_OUT = "SOLD_OUT"
local SUCCESS = "SUCCESS"
local NOT_READY = "NOT_READY"
local NOT_STARTED = "NOT_STARTED"
local ENDED = "ENDED"

local stockKey = KEYS[1]
local issuedUsersKey = KEYS[2]
local issueStartAtKey = KEYS[3]
local issueEndAtKey = KEYS[4]

local userId = ARGV[1]
local nowMillis = tonumber(ARGV[2])

local issueStartAt = tonumber(redis.call('GET', issueStartAtKey))
local issueEndAt = tonumber(redis.call('GET', issueEndAtKey))
local stock = tonumber(redis.call('GET', stockKey))

if issueStartAt == nil or issueEndAt == nil or stock == nil then
    return NOT_READY
end

if nowMillis < issueStartAt then
    return NOT_STARTED
end

if nowMillis >= issueEndAt then
    return ENDED
end

if redis.call('SISMEMBER', issuedUsersKey, userId) == 1 then
	return ALREADY_ISSUED
end

if stock <= 0 then
    return SOLD_OUT
end

redis.call('DECR', stockKey)
redis.call('SADD', issuedUsersKey, userId)

return SUCCESS

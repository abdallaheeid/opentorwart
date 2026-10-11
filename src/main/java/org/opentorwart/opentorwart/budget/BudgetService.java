package org.opentorwart.opentorwart.budget;

import org.opentorwart.opentorwart.Exceptions.BudgetExceededException;
import org.opentorwart.opentorwart.config.BudgetProperties;
import org.opentorwart.opentorwart.config.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class BudgetService {

    private static final Logger log = LoggerFactory.getLogger(BudgetService.class);
    private static final Duration TTL = Duration.ofDays(40);

    private final StringRedisTemplate redis;
    private final RedisScript<Long> reserveScript;
    private final BudgetProperties props;

    public BudgetService(StringRedisTemplate redis, RedisScript<Long> reserveScript,
                         BudgetProperties props) {
        this.redis = redis;
        this.reserveScript = reserveScript;
        this.props = props;
    }

    public Reservation reserve(String team, long estimatedTokens) {
        Long limit = props.teams().get(team);
        if (limit == null) {
            throw new BudgetExceededException(team);              // fail closed: no budget configured
        }

        String key = keyFor(team, currentMonth());
        try {
            Long result = redis.execute(reserveScript, List.of(key),
                    String.valueOf(estimatedTokens),
                    String.valueOf(limit),
                    String.valueOf(TTL.toSeconds()));

            if (result == null || result < 0) {
                throw new BudgetExceededException(team);
            }
            return new Reservation(team, key, estimatedTokens);

        } catch (DataAccessException e) {
            if (!props.failOpen()) {
                throw e;
            }
            log.warn("Redis unavailable, budget check skipped for team={}", team, e);
            return new Reservation(team, key, 0);                 // nothing reserved
        }
    }

    public void settle(Reservation reservation, long actualTokens) {
        long delta = actualTokens - reservation.reservedTokens();
        if (delta == 0) {
            return;
        }
        try {
            redis.opsForValue().increment(reservation.key(), delta);
            redis.expire(reservation.key(), TTL);                 // key may be new (fail-open case)
        } catch (DataAccessException e) {
            log.warn("Budget settle failed for team={} delta={}", reservation.team(), delta, e);
        }
    }

    public boolean isAboveSoftCap(String team) {
        Long limit = props.teams().get(team);
        if (limit == null) {
            return false;
        }
        try {
            String value = redis.opsForValue().get(keyFor(team, currentMonth()));
            long used = value == null ? 0 : Long.parseLong(value);
            return used > limit * props.softCapPercent() / 100;
        } catch (DataAccessException e) {
            return false;
        }
    }

    private static YearMonth currentMonth() {
        return YearMonth.now(ZoneOffset.UTC);
    }

    private static String keyFor(String team, YearMonth month) {
        return "budget:" + team + ":" + month;
    }
}

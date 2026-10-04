package org.opentorwart.opentorwart.budget;

import org.opentorwart.opentorwart.Exceptions.BudgetExceededException;
import org.opentorwart.opentorwart.config.BudgetProperties;
import org.opentorwart.opentorwart.config.Reservation;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BudgetService {

    private final BudgetProperties props;
    private final Map<String, AtomicLong> usedTokens = new ConcurrentHashMap<>();

    public BudgetService(BudgetProperties props) {
        this.props = props;
    }

    /** Reserves tokens atomically, or throws BudgetExceededException if they don't fit. */
    public Reservation reserve(String team, long estimatedTokens) {
        Long limit = props.teams().get(team);
        if (limit == null) {
            throw new BudgetExceededException(team);      // no budget configured → deny
        }

        AtomicLong counter = usedTokens.computeIfAbsent(team, k -> new AtomicLong());

        while (true) {
            long current = counter.get();                  // 1. read
            long next = current + estimatedTokens;         // 2. compute
            if (next > limit) {
                throw new BudgetExceededException(team);   // 3. check
            }
            if (counter.compareAndSet(current, next)) {    // 4. write, only if unchanged
                return new Reservation(team, estimatedTokens);
            }
            // another thread changed the value between 1 and 4 → retry with a fresh read
        }
    }

    /** Releases the reservation and charges the actual usage instead. */
    public void settle(Reservation reservation, long actualTokens) {
        AtomicLong counter = usedTokens.get(reservation.team());
        if (counter == null) {
            return;                       // nothing was reserved, so nothing to settle
        }
        counter.addAndGet(actualTokens - reservation.reservedTokens());
    }

    /** True if the team has used more than softCapPercent of its budget. */
    public boolean isAboveSoftCap(String team) {
        Long limit = props.teams().get(team);
        if (limit == null) {
            return false;                 // no budget configured → assume not above soft cap
        }
        AtomicLong counter = usedTokens.get(team);
        if (counter == null) {
            return false;                 // no tokens used → assume not above soft cap
        }
        return counter.get() > limit * props.softCapPercent() / 100;
    }
}

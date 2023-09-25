/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import java.time.Clock;
import java.time.Instant;

public class OutboxPollingMassIndexingInstructions {
	private final Clock clock;
	final Instant expiration;
	boolean considerEventProcessingSuspended;

	public OutboxPollingMassIndexingInstructions(Clock clock, Instant expiration, boolean considerEventProcessingSuspended) {
		this.clock = clock;
		this.expiration = expiration;
		this.considerEventProcessingSuspended = considerEventProcessingSuspended;
	}

	boolean isStillValid() {
		return timeInMillisecondsToExpiration() > 0;
	}

	long timeInMillisecondsToExpiration() {
		return Math.max( 0L, expiration.toEpochMilli() - clock.millis() );
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

final class OutboxPollingEventProcessingInstructions {
	private final Clock clock;
	final Instant expiration;
	final Optional<OutboxEventFinder> eventFinder;

	public OutboxPollingEventProcessingInstructions(Clock clock, Instant expiration, Optional<OutboxEventFinder> eventFinder) {
		this.clock = clock;
		this.expiration = expiration;
		this.eventFinder = eventFinder;
	}

	boolean isStillValid() {
		return timeInMillisecondsToExpiration() > 0;
	}

	long timeInMillisecondsToExpiration() {
		return Math.max( 0L, expiration.toEpochMilli() - clock.millis() );
	}
}

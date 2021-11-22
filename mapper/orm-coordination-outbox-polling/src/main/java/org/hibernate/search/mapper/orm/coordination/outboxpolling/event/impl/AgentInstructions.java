/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

final class AgentInstructions {
	private final Clock clock;
	final Instant expiration;
	final Optional<OutboxEventFinder> eventFinder;

	public AgentInstructions(Clock clock, Instant expiration, Optional<OutboxEventFinder> eventFinder) {
		this.clock = clock;
		this.expiration = expiration;
		this.eventFinder = eventFinder;
	}

	boolean isStillValid() {
		return clock.instant().isBefore( expiration );
	}
}

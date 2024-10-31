/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import java.time.Duration;

import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.ConfigurationLog;

final class OutboxConfigUtils {


	private OutboxConfigUtils() {
	}

	static Duration checkPulseInterval(Duration pulseInterval, Duration pollingInterval) {
		if ( pulseInterval.compareTo( pollingInterval ) < 0 ) {
			throw ConfigurationLog.INSTANCE.invalidPollingIntervalAndPulseInterval( pollingInterval.toMillis() );
		}
		return pulseInterval;
	}

	static Duration checkPulseExpiration(Duration pulseExpiration, Duration pulseInterval) {
		Duration pulseIntervalTimes3 = pulseInterval.multipliedBy( 3 );
		if ( pulseExpiration.compareTo( pulseIntervalTimes3 ) < 0 ) {
			throw ConfigurationLog.INSTANCE.invalidPulseIntervalAndPulseExpiration( pulseIntervalTimes3.toMillis() );
		}
		return pulseExpiration;
	}
}

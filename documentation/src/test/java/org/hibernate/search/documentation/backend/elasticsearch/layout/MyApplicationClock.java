/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.backend.elasticsearch.layout;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public final class MyApplicationClock {

	private static final Clock INSTANCE = Clock.fixed( Instant.parse( "2017-11-06T19:19:00.000Z" ), ZoneOffset.UTC );

	public static Clock get() {
		return INSTANCE;
	}

	private MyApplicationClock() {
	}
}

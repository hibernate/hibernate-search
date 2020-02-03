/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

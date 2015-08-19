/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.Duration;
import java.util.Locale;

import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Converts a {@link Duration} to a {@link String}.
 * <p>
 * The string is obtained concatenating the number of seconds with the nano of seconds.
 * The values are padded with 0 to allow field sorting.
 *
 * @author Davide D'Alto
 */
public class DurationBridge implements TwoWayStringBridge {

	private static final Log log = LoggerFactory.make();
	private static final int SECONDS_PADDING = 20;
	private static final int NANOS_PADDING = 9;
	private static final String FORMAT = "%+0" + SECONDS_PADDING + "d%0" + NANOS_PADDING + "d";

	public static final DurationBridge INSTANCE = new DurationBridge();

	private DurationBridge() {
	}

	@Override
	public String objectToString(Object object) {
		if ( object == null ) {
			return null;
		}

		Duration duration = (Duration) object;
		long seconds = duration.getSeconds();
		int nano = duration.getNano();
		return String.format( Locale.ROOT, FORMAT, seconds, nano );
	}

	@Override
	public Object stringToObject(String stringValue) {
		if ( stringValue == null ) {
			return null;
		}

		try {
			long seconds = Long.parseLong( stringValue.substring( 0, SECONDS_PADDING ) );
			int nano = Integer.parseInt( stringValue.substring( SECONDS_PADDING ) );
			return Duration.ofSeconds( seconds, nano );
		}
		catch (NumberFormatException e) {
			throw log.parseException( stringValue, Duration.class, e );
		}
	}
}

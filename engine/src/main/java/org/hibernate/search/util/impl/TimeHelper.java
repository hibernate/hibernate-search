/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

/**
 * Helpers for classes in java.time.*
 *
 * @author Yoann Rodiere
 */
public final class TimeHelper {

	private TimeHelper() {
		// not allowed
	}

	/**
	 * Workaround for https://bugs.openjdk.java.net/browse/JDK-8066982,
	 * which at the moment has only been solved for JDK9.
	 *
	 * <p>Tested against TCKDTFParsedInstant in
	 * http://hg.openjdk.java.net/jdk9/dev/jdk/rev/f371bdfb7875
	 * with both JDK8b101 and JDK9 (early access - build 137).
	 *
	 * @param value The value to be parsed
	 * @param formatter The formatter to use when parsing
	 * @return The parsed {@link ZonedDateTime}
	 */
	public static ZonedDateTime parseZoneDateTime(String value, DateTimeFormatter formatter) {
		TemporalAccessor temporal = formatter.parse( value );
		if ( !temporal.isSupported( ChronoField.OFFSET_SECONDS ) ) {
			// No need for a workaround
			return ZonedDateTime.from( temporal );
		}
		/*
		 * Rationale
		 *
		 * The bug lies in the way epoch seconds are retrieved: the date is interpreted as being
		 * expressed in the default offset for the timezone, instead of using the given offset.
		 * ZonedDateTime.from uses these epoch seconds in priority when retrieving the local date/time,
		 * and falls back to using the epoch day and nano of day.
		 * We work around the bug by bypassing ZonedDateTime.from and not using the epoch seconds,
		 * but instead directly retrieving the LocalDateTime using LocalDateTime.from. This method
		 * relies on ChronoField.EPOCH_DAY and ChronoField.NANO_OF_DAY, which (strangely) seem
		 * unaffected by the bug.
		 */
		ZoneId zone = ZoneId.from( temporal );
		ZoneOffset offset = ZoneOffset.from( temporal );
		LocalDateTime ldt = LocalDateTime.from( temporal );
		return ZonedDateTime.ofInstant( ldt, offset, zone );
	}

}

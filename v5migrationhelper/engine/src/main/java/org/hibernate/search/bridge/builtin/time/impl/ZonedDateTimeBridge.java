/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

import org.hibernate.search.util.impl.TimeHelper;

/**
 * Converts a {@link ZonedDateTime} to a {@link String}.
 * <p>
 * A {@code ZonedDateTime} 2012-12-31T23:59:59.999+01:00 Europe/Paris becomes the string
 * {@code +0000020121231235959000000999+01:00Europe/Paris}.
 * <p>
 * The sign is always present for the year and the string is padded with 0 to allow field sorting.
 *
 * @author Davide D'Alto
 */
public class ZonedDateTimeBridge extends TemporalAccessorStringBridge<ZonedDateTime> {

	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.append( LocalDateTimeBridge.FORMATTER )
			/*
			 * We include both the offset and the zone region ID, in order to handle
			 * dates matching multiple instants, which happens when clocks are set back
			 * due to switching daylight saving time.
			 *
			 * Both the offset and zone region ID are optional when parsing.
			 * That's for two reasons.
			 *
			 * 1. The previous format, in Hibernate Search 5.5, used ".appendZoneId",
			 * which may be the offset or the zone region ID, depending on how the
			 * ZonedDateTime was built. Even in legacy format, we nevertheless expect
			 * either the zone region ID or the offset to be provided.
			 * 2. Depending on how a ZonedDateTime is built, there may be no zone region
			 * ID to output, so we must not make this part of the string mandatory. It is
			 * the case in particular when using ZoneOffsets to build ZonedDateTimes.
			 *
			 * See HSEARCH-2363 for more information.
			 */
			.optionalStart()
			.appendOffsetId()
			.optionalEnd()
			.optionalStart()
			.parseCaseSensitive()
			.appendZoneRegionId()
			.optionalEnd()
			.toFormatter();

	public static final ZonedDateTimeBridge INSTANCE = new ZonedDateTimeBridge();

	private ZonedDateTimeBridge() {
		super( FORMATTER, ZonedDateTime.class );
	}

	@Override
	ZonedDateTime parse(String stringValue) throws DateTimeParseException {
		return TimeHelper.parseZoneDateTime( stringValue, FORMATTER );
	}
}

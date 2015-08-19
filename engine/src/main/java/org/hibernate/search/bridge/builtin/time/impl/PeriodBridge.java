/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.Duration;
import java.time.Period;
import java.util.Locale;

import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Converts a {@link Period} to a {@link String} concatenating year, months and days.
 * <p>
 * The sign is always present for the year and the string is padded with 0 to allow field sorting.
 *
 * @author Davide D'Alto
 */
public class PeriodBridge implements TwoWayStringBridge {

	private static final Log log = LoggerFactory.make();
	private static final int PADDING = 11;
	private static final String FORMAT = "%+0" + PADDING + "d%+0" + PADDING + "d%+0" + PADDING + "d";

	public static final PeriodBridge INSTANCE = new PeriodBridge();

	private PeriodBridge() {
	}

	@Override
	public String objectToString(Object object) {
		if ( object == null ) {
			return null;
		}

		Period period = (Period) object;
		int years = period.getYears();
		int months = period.getMonths();
		int days = period.getDays();
		return String.format( Locale.ROOT, FORMAT, years, months, days );
	}

	@Override
	public Object stringToObject(String stringValue) {
		if ( stringValue == null ) {
			return null;
		}

		try {
			int years = Integer.parseInt( stringValue.substring( 0, PADDING ) );
			int months = Integer.parseInt( stringValue.substring( PADDING, 2 * PADDING ) );
			int days = Integer.parseInt( stringValue.substring( 2 * PADDING ) );
			return Period.of( years, months, days );
		}
		catch (NumberFormatException e) {
			throw log.parseException( stringValue, Duration.class, e );
		}
	}
}

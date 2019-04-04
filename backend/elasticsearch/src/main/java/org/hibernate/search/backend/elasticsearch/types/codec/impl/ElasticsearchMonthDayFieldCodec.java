/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;

public class ElasticsearchMonthDayFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<MonthDay> {

	/**
	 * Elasticsearch cannot store a monthday, it always stores an actual date.
	 * If we omit the year, it's automatically set to 1970.
	 * 1970 is not a leap year, so this would prevent us from indexing february 29th.
	 * Thus we explicitly set the year to a leap year for all indexed values.
	 * <p>
	 * Note we must do it consistently for all indexed values, not just february 29th,
	 * otherwise range predicates and sorts would not work correctly.
	 * <p>
	 * WARNING: changing this value will force applications to reindex all data
	 * to avoid problems with sorts and range predicates.
	 */
	private static final int LEAP_YEAR = 0; // Year 0 exists in the proleptic gregorian calendar and is a leap year.

	public ElasticsearchMonthDayFieldCodec(DateTimeFormatter delegate) {
		super( delegate );
	}

	@Override
	protected String nullUnsafeFormat(MonthDay value) {
		LocalDate date = value.atYear( LEAP_YEAR );
		return formatter.format( date );
	}

	@Override
	protected MonthDay nullUnsafeParse(String stringValue) {
		LocalDate date = LocalDate.parse( stringValue, formatter );
		return MonthDay.from( date );
	}
}

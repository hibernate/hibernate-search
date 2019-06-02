/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;

public class ElasticsearchOffsetTimeFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<OffsetTime> {

	private static final LocalDate EPOCH_DATE = LocalDate.of( 1970, Month.JANUARY, 1 );

	public ElasticsearchOffsetTimeFieldCodec(DateTimeFormatter delegate) {
		super( delegate );
	}

	@Override
	protected OffsetTime nullUnsafeParse(String stringValue) {
		return OffsetTime.parse( stringValue, formatter );
	}

	@Override
	protected Long nullUnsafeScalar(OffsetTime value) {
		return value.atDate( EPOCH_DATE ).toInstant().toEpochMilli();
	}
}

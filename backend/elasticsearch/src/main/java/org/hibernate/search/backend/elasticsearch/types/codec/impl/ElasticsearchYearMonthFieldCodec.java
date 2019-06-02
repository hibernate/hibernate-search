/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class ElasticsearchYearMonthFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<YearMonth> {

	public ElasticsearchYearMonthFieldCodec(DateTimeFormatter delegate) {
		super( delegate );
	}

	@Override
	protected YearMonth nullUnsafeParse(String stringValue) {
		return YearMonth.parse( stringValue, formatter );
	}

	@Override
	protected Long nullUnsafeScalar(YearMonth value) {
		return value.atDay( 1 ).atStartOfDay().toInstant( ZoneOffset.UTC ).toEpochMilli();
	}
}

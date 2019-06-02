/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.Year;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class ElasticsearchYearFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<Year> {

	public ElasticsearchYearFieldCodec(DateTimeFormatter delegate) {
		super( delegate );
	}

	@Override
	protected Year nullUnsafeParse(String stringValue) {
		return Year.parse( stringValue, formatter );
	}

	@Override
	protected Long nullUnsafeScalar(Year value) {
		return value.atDay( 1 ).atStartOfDay().toInstant( ZoneOffset.UTC ).toEpochMilli();
	}
}

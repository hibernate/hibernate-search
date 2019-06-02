/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class ElasticsearchLocalDateFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<LocalDate> {

	public ElasticsearchLocalDateFieldCodec(DateTimeFormatter delegate) {
		super( delegate );
	}

	@Override
	protected LocalDate nullUnsafeParse(String stringValue) {
		return LocalDate.parse( stringValue, formatter );
	}

	@Override
	protected Long nullUnsafeScalar(LocalDate value) {
		return value.atStartOfDay( ZoneOffset.UTC ).toInstant().toEpochMilli();
	}
}

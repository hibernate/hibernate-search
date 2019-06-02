/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class ElasticsearchLocalDateTimeFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<LocalDateTime> {

	public ElasticsearchLocalDateTimeFieldCodec(DateTimeFormatter delegate) {
		super( delegate );
	}

	@Override
	protected LocalDateTime nullUnsafeParse(String stringValue) {
		return LocalDateTime.parse( stringValue, formatter );
	}

	@Override
	protected Long nullUnsafeScalar(LocalDateTime value) {
		return value.toInstant( ZoneOffset.UTC ).toEpochMilli();
	}
}

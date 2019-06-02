/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class ElasticsearchInstantFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<Instant> {

	public ElasticsearchInstantFieldCodec(DateTimeFormatter delegate) {
		super( delegate );
	}

	@Override
	protected String nullUnsafeFormat(Instant value) {
		return formatter.format( value.atOffset( ZoneOffset.UTC ) );
	}

	@Override
	protected Instant nullUnsafeParse(String stringValue) {
		return formatter.parse( stringValue, OffsetDateTime::from ).toInstant();
	}

	@Override
	protected Long nullUnsafeScalar(Instant value) {
		return value.toEpochMilli();
	}
}

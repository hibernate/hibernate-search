/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class ElasticsearchOffsetDateTimeFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<OffsetDateTime> {

	public ElasticsearchOffsetDateTimeFieldCodec(DateTimeFormatter delegate) {
		super( delegate );
	}

	@Override
	protected OffsetDateTime nullUnsafeParse(String stringValue) {
		return OffsetDateTime.parse( stringValue, formatter );
	}

	@Override
	protected Long nullUnsafeScalar(OffsetDateTime value) {
		return value.toInstant().toEpochMilli();
	}
}

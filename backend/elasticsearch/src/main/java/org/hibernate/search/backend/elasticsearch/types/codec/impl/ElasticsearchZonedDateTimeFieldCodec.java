/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.util.common.impl.TimeHelper;

public class ElasticsearchZonedDateTimeFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<ZonedDateTime> {

	public ElasticsearchZonedDateTimeFieldCodec(DateTimeFormatter delegate) {
		super( delegate );
	}

	@Override
	protected ZonedDateTime nullUnsafeParse(String stringValue) {
		return TimeHelper.parseZoneDateTime( stringValue, formatter );
	}

	@Override
	protected Long nullUnsafeScalar(ZonedDateTime value) {
		return value.toInstant().toEpochMilli();
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;

public class ElasticsearchOffsetTimeFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<OffsetTime> {

	public ElasticsearchOffsetTimeFieldCodec(DateTimeFormatter delegate) {
		super( delegate );
	}

	@Override
	protected OffsetTime nullUnsafeParse(String stringValue) {
		return OffsetTime.parse( stringValue, formatter );
	}
}

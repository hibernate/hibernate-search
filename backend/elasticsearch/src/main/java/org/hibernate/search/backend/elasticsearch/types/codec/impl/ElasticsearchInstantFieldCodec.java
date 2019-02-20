/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class ElasticsearchInstantFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<Instant> {

	public ElasticsearchInstantFieldCodec(DateTimeFormatter delegate) {
		super( delegate );
	}

	@Override
	protected Instant nullUnsafeParse(String stringValue) {
		return formatter.parse( stringValue, Instant::from );
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.nulls.codec.impl;

import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.bridge.spi.NullMarker;

/**
 * @author Sanne Grinovero
 */
public class ElasticsearchDoubleNullMarkerCodec extends ElasticsearchAsNullNullMarkerCodec {

	public ElasticsearchDoubleNullMarkerCodec(NullMarker nullMarker) {
		super( nullMarker );
	}

	@Override
	public Query createNullMatchingQuery(String fieldName) {
		Double nullEncoded = (Double) nullMarker.nullEncoded();
		return NumericRangeQuery.newDoubleRange( fieldName, nullEncoded, nullEncoded, true, true );
	}

}

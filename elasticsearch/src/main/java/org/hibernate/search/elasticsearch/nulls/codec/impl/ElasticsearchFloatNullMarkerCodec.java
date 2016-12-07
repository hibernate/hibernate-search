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
import org.hibernate.search.elasticsearch.nulls.impl.ElasticsearchNullMarkerIndexStrategy;

/**
 * @author Sanne Grinovero
 */
public class ElasticsearchFloatNullMarkerCodec extends ElasticsearchNullMarkerCodec {

	public ElasticsearchFloatNullMarkerCodec(final NullMarker nullMarker, ElasticsearchNullMarkerIndexStrategy indexStrategy) {
		super( nullMarker, indexStrategy );
	}

	@Override
	public Query createNullMatchingQuery(String fieldName) {
		Float nullEncoded = (Float) nullMarker.nullEncoded();
		return NumericRangeQuery.newFloatRange( fieldName, nullEncoded, nullEncoded, true, true );
	}

}

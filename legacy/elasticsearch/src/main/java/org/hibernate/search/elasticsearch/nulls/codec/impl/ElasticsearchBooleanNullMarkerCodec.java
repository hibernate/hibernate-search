/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.nulls.codec.impl;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;
import org.hibernate.search.bridge.spi.NullMarker;

/**
 * @author Sanne Grinovero
 */
public class ElasticsearchBooleanNullMarkerCodec extends ElasticsearchAsNullNullMarkerCodec {

	private final BytesRef encodedToken;

	public ElasticsearchBooleanNullMarkerCodec(NullMarker nullMarker) {
		super( nullMarker );
		/*
		 * Booleans are queried as strings because Lucene Queries lack a concept of boolean term
		 * (we do the same for non-nulls)
		 *
		 * See HSEARCH-2498.
		 */
		this.encodedToken = new BytesRef( nullMarker.nullEncoded().toString() );
	}

	@Override
	public Query createNullMatchingQuery(String fieldName) {
		return new TermQuery( new Term( fieldName, encodedToken ) );
	}

}

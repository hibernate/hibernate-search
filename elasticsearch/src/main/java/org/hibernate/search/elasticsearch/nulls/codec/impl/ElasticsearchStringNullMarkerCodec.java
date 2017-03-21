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
import org.hibernate.search.elasticsearch.nulls.impl.ElasticsearchNullMarkerIndexStrategy;

/**
 * @author Sanne Grinovero
 */
public class ElasticsearchStringNullMarkerCodec extends ElasticsearchNullMarkerCodec {

	private final BytesRef encodedToken;

	public ElasticsearchStringNullMarkerCodec(NullMarker nullMarker, ElasticsearchNullMarkerIndexStrategy indexStrategy) {
		super( nullMarker, indexStrategy );
		this.encodedToken = new BytesRef( (String) nullMarker.nullEncoded() );
	}

	@Override
	public Query createNullMatchingQuery(String fieldName) {
		return new TermQuery( new Term( fieldName, encodedToken ) );
	}

}

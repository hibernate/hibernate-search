/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.nulls.codec.impl;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.spi.NullMarker;
import org.hibernate.search.elasticsearch.nulls.impl.ElasticsearchNullMarkerField;
import org.hibernate.search.elasticsearch.nulls.impl.ElasticsearchNullMarkerIndexStrategy;
import org.hibernate.search.engine.nulls.codec.impl.NullMarkerCodec;


/**
 * @author Yoann Rodiere
 */
abstract class ElasticsearchNullMarkerCodec implements NullMarkerCodec {

	protected final NullMarker nullMarker;

	protected final ElasticsearchNullMarkerIndexStrategy indexStrategy;

	public ElasticsearchNullMarkerCodec(NullMarker nullMarker, ElasticsearchNullMarkerIndexStrategy indexStrategy) {
		super();
		this.nullMarker = nullMarker;
		this.indexStrategy = indexStrategy;
	}

	@Override
	public NullMarker getNullMarker() {
		return nullMarker;
	}

	@Override
	public void encodeNullValue(String fieldName, Document document, LuceneOptions luceneOptions) {
		document.add( new ElasticsearchNullMarkerField( fieldName, nullMarker, indexStrategy ) );
	}

	@Override
	public boolean representsNullValue(IndexableField field) {
		/*
		 * Never true:
		 *
		 * When using the DEFAULT index strategy, null value are null in the _source when projecting,
		 * so we can't have an IndexableField representing a null value in the resulting
		 * document.
		 *
		 * When using the CONTAINER index strategy, the codec is being used for a container,
		 * and projecting on such fields is explicitly not supported in the docs.
		 */
		return false;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + nullMarker + "]";
	}

}

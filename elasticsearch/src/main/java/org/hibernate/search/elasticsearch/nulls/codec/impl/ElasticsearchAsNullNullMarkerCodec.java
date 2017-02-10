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
import org.hibernate.search.engine.nulls.codec.impl.NullMarkerCodec;


/**
 * A base class for {@link NullMarkerCodec}s that index null values as the JSON "null" value.
 *
 * @author Yoann Rodiere
 */
public abstract class ElasticsearchAsNullNullMarkerCodec implements NullMarkerCodec {

	protected final NullMarker nullMarker;

	public ElasticsearchAsNullNullMarkerCodec(NullMarker nullMarker) {
		super();
		this.nullMarker = nullMarker;
	}

	@Override
	public NullMarker getNullMarker() {
		return nullMarker;
	}

	@Override
	public void encodeNullValue(String fieldName, Document document, LuceneOptions luceneOptions) {
		document.add( new ElasticsearchNullMarkerField( fieldName, nullMarker ) );
	}

	@Override
	public boolean representsNullValue(IndexableField field) {
		/*
		 * Never true: null value are absent from _source when projecting, so we can't have an
		 * IndexableField representing a null value in the resulting document.
		 */
		return false;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + nullMarker + "]";
	}

}

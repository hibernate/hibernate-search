/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl.nullencoding;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * Implementation of NullMarkerCodec which implements the no-op strategy
 * of not writing anything to the index for null values.
 *
 * @author Sanne Grinovero
 */
public class NotEncodingCodec implements NullMarkerCodec {

	public static NotEncodingCodec SINGLETON = new NotEncodingCodec();

	private NotEncodingCodec() {
		// do not instantiate, use the SINGLETON
	}

	@Override
	public String nullRepresentedAsString() {
		return null;
	}

	@Override
	public void encodeNullValue(String fieldName, Document document, LuceneOptions luceneOptions) {
		// no-op
	}

	@Override
	public Query createNullMatchingQuery(String fieldName) {
		// no-op
		return null;
	}

	@Override
	public boolean representsNullValue(IndexableField field) {
		return field == null;
	}

}

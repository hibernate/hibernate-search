/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneFloatDomain;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneFloatFieldCodec extends AbstractLuceneNumericFieldCodec<Float, Float> {

	public LuceneFloatFieldCodec(Indexing indexing, DocValues docValues, Storage storage,
			Float indexNullAsValue) {
		super( indexing, docValues, storage, indexNullAsValue );
	}

	@Override
	void addStoredToDocument(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, Float value,
			Float encodedValue) {
		documentBuilder.addField( new StoredField( absoluteFieldPath, encodedValue ) );
	}

	@Override
	public Float decode(IndexableField field) {
		return (Float) field.numericValue();
	}

	@Override
	public Float encode(Float value) {
		return value;
	}

	@Override
	public Float decode(Float encoded) {
		return encoded;
	}

	@Override
	public LuceneNumericDomain<Float> getDomain() {
		return LuceneFloatDomain.get();
	}
}

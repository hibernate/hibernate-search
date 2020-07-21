/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneDoubleDomain;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneDoubleFieldCodec extends AbstractLuceneNumericFieldCodec<Double, Double> {

	public LuceneDoubleFieldCodec(boolean projectable, boolean searchable, boolean sortable,
			boolean aggregable, Double indexNullAsValue) {
		super( projectable, searchable, sortable, aggregable, indexNullAsValue );
	}

	@Override
	void addStoredToDocument(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, Double value,
			Double encodedValue) {
		documentBuilder.addField( new StoredField( absoluteFieldPath, encodedValue ) );
	}

	@Override
	public Double decode(IndexableField field) {
		return (Double) field.numericValue();
	}

	@Override
	public Double encode(Double value) {
		return value;
	}

	@Override
	public Double decode(Double encoded) {
		return encoded;
	}

	@Override
	public LuceneNumericDomain<Double> getDomain() {
		return LuceneDoubleDomain.get();
	}
}

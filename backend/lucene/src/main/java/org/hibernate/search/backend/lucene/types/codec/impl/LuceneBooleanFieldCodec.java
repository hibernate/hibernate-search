/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneIntegerDomain;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneBooleanFieldCodec extends AbstractLuceneNumericFieldCodec<Boolean, Integer> {

	public LuceneBooleanFieldCodec(boolean projectable, boolean searchable, boolean sortable,
			boolean aggregable, Boolean indexNullAsValue) {
		super( projectable, searchable, sortable, aggregable, indexNullAsValue );
	}

	@Override
	void doEncodeForProjection(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, Boolean value,
			Integer encodedValue) {
		documentBuilder.addField( new StoredField( absoluteFieldPath, encodedValue ) );
	}

	@Override
	public Boolean decode(IndexableField field) {
		Integer intValue = (Integer) field.numericValue();
		return ( intValue > 0 );
	}

	@Override
	public Integer encode(Boolean value) {
		return value ? 1 : 0;
	}

	@Override
	public Boolean decode(Integer encoded) {
		return encoded > 0;
	}

	@Override
	public LuceneNumericDomain<Integer> getDomain() {
		return LuceneIntegerDomain.get();
	}

}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneIntegerDomain;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

public final class LuceneIntegerFieldCodec extends AbstractLuceneNumericFieldCodec<Integer, Integer> {

	public LuceneIntegerFieldCodec(boolean projectable, boolean searchable, boolean sortable,
			boolean aggregable, Integer indexNullAsValue) {
		super( projectable, searchable, sortable, aggregable, indexNullAsValue );
	}

	@Override
	void addStoredToDocument(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, Integer value,
			Integer encodedValue) {
		documentBuilder.addField( new StoredField( absoluteFieldPath, encodedValue ) );
	}

	@Override
	public Integer decode(IndexableField field) {
		return (Integer) field.numericValue();
	}

	@Override
	public Integer encode(Integer value) {
		return value;
	}

	@Override
	public Integer decode(Integer encoded) {
		return encoded;
	}

	@Override
	public LuceneNumericDomain<Integer> getDomain() {
		return LuceneIntegerDomain.get();
	}
}

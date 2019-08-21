/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.time.Year;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneIntegerDomain;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneYearFieldCodec extends AbstractLuceneNumericFieldCodec<Year, Integer> {

	public LuceneYearFieldCodec(boolean projectable, boolean searchable, boolean sortable,
			boolean aggregable, Year indexNullAsValue) {
		super( projectable, searchable, sortable, aggregable, indexNullAsValue );
	}

	@Override
	void doEncodeForProjection(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, Year value,
			Integer encodedValue) {
		documentBuilder.addField( new StoredField( absoluteFieldPath, encodedValue ) );
	}

	@Override
	public Year decode(Document document, String absoluteFieldPath) {
		IndexableField field = document.getField( absoluteFieldPath );

		if ( field == null ) {
			return null;
		}

		Integer integer = (Integer) field.numericValue();
		return Year.of( integer );
	}

	@Override
	public Integer encode(Year value) {
		return value.getValue();
	}

	@Override
	public Year decode(Integer encoded) {
		return Year.of( encoded );
	}

	@Override
	public LuceneNumericDomain<Integer> getDomain() {
		return LuceneIntegerDomain.get();
	}
}

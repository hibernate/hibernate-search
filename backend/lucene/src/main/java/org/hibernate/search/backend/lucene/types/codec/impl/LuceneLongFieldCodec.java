/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneLongDomain;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneLongFieldCodec extends AbstractLuceneNumericFieldCodec<Long, Long> {

	public LuceneLongFieldCodec(boolean projectable, boolean searchable, boolean sortable,
			boolean aggregable, Long indexNullAsValue) {
		super( projectable, searchable, sortable, aggregable, indexNullAsValue );
	}

	@Override
	void doEncodeForProjection(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, Long value,
			Long encodedValue) {
		documentBuilder.addField( new StoredField( absoluteFieldPath, encodedValue ) );
	}

	@Override
	public Long decode(Document document, String absoluteFieldPath) {
		IndexableField field = document.getField( absoluteFieldPath );

		if ( field == null ) {
			return null;
		}

		return (Long) field.numericValue();
	}

	@Override
	public Long encode(Long value) {
		return value;
	}

	@Override
	public Long decode(Long encoded) {
		return encoded;
	}

	@Override
	public LuceneNumericDomain<Long> getDomain() {
		return LuceneLongDomain.get();
	}
}

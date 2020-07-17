/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

public abstract class AbstractLuceneNumericFieldCodec<F, E extends Number>
		implements LuceneStandardFieldCodec<F, E> {

	private final boolean projectable;
	private final boolean searchable;
	private final boolean sortable;
	private final boolean aggregable;

	private final F indexNullAsValue;

	public AbstractLuceneNumericFieldCodec(boolean projectable, boolean searchable, boolean sortable,
			boolean aggregable, F indexNullAsValue) {
		this.projectable = projectable;
		this.searchable = searchable;
		this.sortable = sortable;
		this.aggregable = aggregable;
		this.indexNullAsValue = indexNullAsValue;
	}

	@Override
	public final void addToDocument(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, F value) {
		if ( value == null && indexNullAsValue != null ) {
			value = indexNullAsValue;
		}

		if ( value == null ) {
			return;
		}

		E encodedValue = encode( value );

		if ( projectable ) {
			addStoredToDocument( documentBuilder, absoluteFieldPath, value, encodedValue );
		}

		LuceneNumericDomain<E> domain = getDomain();

		if ( sortable || aggregable ) {
			documentBuilder.addField( domain.createSortedDocValuesField( absoluteFieldPath, encodedValue ) );
		}
		else {
			// For the "exists" predicate
			documentBuilder.addFieldName( absoluteFieldPath );
		}

		if ( searchable ) {
			documentBuilder.addField( domain.createIndexField( absoluteFieldPath, encodedValue ) );
		}
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		return getClass() == obj.getClass();
	}

	public abstract F decode(E encoded);

	public abstract LuceneNumericDomain<E> getDomain();

	abstract void addStoredToDocument(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath,
			F value, E encodedValue);

}

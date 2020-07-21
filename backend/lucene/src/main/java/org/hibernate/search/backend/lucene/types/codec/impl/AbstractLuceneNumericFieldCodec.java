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

	private final Indexing indexing;
	private final DocValues docValues;
	private final Storage storage;
	private final F indexNullAsValue;

	public AbstractLuceneNumericFieldCodec(Indexing indexing, DocValues docValues, Storage storage,
			F indexNullAsValue) {
		this.indexing = indexing;
		this.docValues = docValues;
		this.storage = storage;
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

		LuceneNumericDomain<E> domain = getDomain();

		if ( Indexing.ENABLED == indexing ) {
			documentBuilder.addField( domain.createIndexField( absoluteFieldPath, encodedValue ) );
		}

		if ( DocValues.ENABLED == docValues ) {
			documentBuilder.addField( domain.createSortedDocValuesField( absoluteFieldPath, encodedValue ) );
		}
		else {
			// For the "exists" predicate
			documentBuilder.addFieldName( absoluteFieldPath );
		}

		if ( Storage.ENABLED == storage ) {
			addStoredToDocument( documentBuilder, absoluteFieldPath, value, encodedValue );
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

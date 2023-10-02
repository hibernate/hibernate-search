/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

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
	public final void addToDocument(LuceneDocumentContent documentBuilder, String absoluteFieldPath, F value) {
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

	abstract void addStoredToDocument(LuceneDocumentContent documentBuilder, String absoluteFieldPath,
			F value, E encodedValue);

}

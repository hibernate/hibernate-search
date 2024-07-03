/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.converter.LuceneFieldContributor;
import org.hibernate.search.backend.lucene.types.converter.LuceneFieldValueExtractor;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.index.IndexableField;

public final class LuceneFieldFieldCodec<F> implements LuceneFieldCodec<F, F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneFieldContributor<F> fieldContributor;
	private final LuceneFieldValueExtractor<F> fieldValueExtractor;
	private final Class<F> valueClass;

	public LuceneFieldFieldCodec(Class<F> valueClass, LuceneFieldContributor<F> fieldContributor,
			LuceneFieldValueExtractor<F> fieldValueExtractor) {
		this.valueClass = valueClass;
		this.fieldContributor = fieldContributor;
		this.fieldValueExtractor = fieldValueExtractor;
	}

	@Override
	public void addToDocument(LuceneDocumentContent documentBuilder, String absoluteFieldPath, F value) {
		if ( value == null ) {
			return;
		}

		fieldContributor.contribute( absoluteFieldPath, value, f -> contributeField( documentBuilder, absoluteFieldPath, f ) );
	}

	@Override
	public F decode(IndexableField field) {
		if ( fieldValueExtractor == null ) {
			// This should not happen as we disable projections when fieldValueExtractor is null
			throw new AssertionFailure( "Native field '" + field.name() + "' lacks a field value extractor" );
		}
		return fieldValueExtractor.extract( field );
	}

	@Override
	public F decode(F field) {
		return field;
	}

	@Override
	public F encode(F value) {
		return value;
	}

	@Override
	public Class<F> encodedType() {
		return valueClass;
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?, ?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( LuceneFieldFieldCodec.class != obj.getClass() ) {
			return false;
		}

		LuceneFieldFieldCodec<?> other = (LuceneFieldFieldCodec<?>) obj;

		return Objects.equals( fieldValueExtractor, other.fieldValueExtractor );
	}

	private static void contributeField(LuceneDocumentContent documentBuilder, String absoluteFieldPath, IndexableField field) {
		if ( !absoluteFieldPath.equals( field.name() ) ) {
			throw log.invalidFieldPath( absoluteFieldPath, field.name() );
		}
		documentBuilder.addField( field );
	}
}

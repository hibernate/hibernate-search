/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.math.BigInteger;

import org.hibernate.search.backend.lucene.logging.impl.MappingLog;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneBigIntegerFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;
import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;

class LuceneBigIntegerIndexFieldTypeOptionsStep
		extends AbstractLuceneNumericIndexFieldTypeOptionsStep<LuceneBigIntegerIndexFieldTypeOptionsStep, BigInteger>
		implements ScaledNumberIndexFieldTypeOptionsStep<LuceneBigIntegerIndexFieldTypeOptionsStep, BigInteger> {

	private final IndexFieldTypeDefaultsProvider defaultsProvider;

	private Integer decimalScale = null;

	LuceneBigIntegerIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext,
			IndexFieldTypeDefaultsProvider defaultsProvider) {
		super( buildContext, BigInteger.class, DefaultStringConverters.BIG_INTEGER );
		this.defaultsProvider = defaultsProvider;
	}

	@Override
	public LuceneBigIntegerIndexFieldTypeOptionsStep decimalScale(int decimalScale) {
		this.decimalScale = decimalScale;
		return this;
	}

	@Override
	protected LuceneBigIntegerIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<BigInteger, ?> createCodec(
			Indexing indexing,
			DocValues docValues,
			Storage storage,
			BigInteger indexNullAsValue) {
		int resolvedDecimalScale = resolveDecimalScale();
		if ( resolvedDecimalScale > 0 ) {
			throw MappingLog.INSTANCE.invalidDecimalScale( resolvedDecimalScale, buildContext.getEventContext() );
		}
		return new LuceneBigIntegerFieldCodec( indexing, docValues, storage, indexNullAsValue, resolvedDecimalScale );
	}

	private int resolveDecimalScale() {
		if ( decimalScale != null ) {
			return decimalScale;
		}
		if ( defaultsProvider.decimalScale() != null ) {
			return defaultsProvider.decimalScale();
		}

		throw MappingLog.INSTANCE.nullDecimalScale( buildContext.hints().missingDecimalScale(),
				buildContext.getEventContext() );
	}
}

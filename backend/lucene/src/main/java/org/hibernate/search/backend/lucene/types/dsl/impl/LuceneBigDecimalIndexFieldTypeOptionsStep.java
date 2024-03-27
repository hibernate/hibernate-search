/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneBigDecimalFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;
import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class LuceneBigDecimalIndexFieldTypeOptionsStep
		extends AbstractLuceneNumericIndexFieldTypeOptionsStep<LuceneBigDecimalIndexFieldTypeOptionsStep, BigDecimal>
		implements ScaledNumberIndexFieldTypeOptionsStep<LuceneBigDecimalIndexFieldTypeOptionsStep, BigDecimal> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexFieldTypeDefaultsProvider defaultsProvider;

	private Integer decimalScale = null;

	LuceneBigDecimalIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext,
			IndexFieldTypeDefaultsProvider defaultsProvider) {
		super( buildContext, BigDecimal.class );
		this.defaultsProvider = defaultsProvider;
	}

	@Override
	public LuceneBigDecimalIndexFieldTypeOptionsStep decimalScale(int decimalScale) {
		this.decimalScale = decimalScale;
		return this;
	}

	@Override
	protected LuceneBigDecimalIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<BigDecimal, ?> createCodec(
			Indexing indexing,
			DocValues docValues,
			Storage storage,
			BigDecimal indexNullAsValue) {
		int resolvedDecimalScale = resolveDecimalScale();
		return new LuceneBigDecimalFieldCodec( indexing, docValues, storage, indexNullAsValue, resolvedDecimalScale );
	}

	private int resolveDecimalScale() {
		if ( decimalScale != null ) {
			return decimalScale;
		}
		if ( defaultsProvider.decimalScale() != null ) {
			return defaultsProvider.decimalScale();
		}

		throw log.nullDecimalScale( buildContext.hints().missingDecimalScale(), buildContext.getEventContext() );
	}
}

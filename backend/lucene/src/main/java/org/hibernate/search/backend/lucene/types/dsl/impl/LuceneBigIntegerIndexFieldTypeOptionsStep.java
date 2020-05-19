/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigInteger;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneBigIntegerFieldCodec;
import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class LuceneBigIntegerIndexFieldTypeOptionsStep
		extends AbstractLuceneNumericIndexFieldTypeOptionsStep<LuceneBigIntegerIndexFieldTypeOptionsStep, BigInteger>
		implements ScaledNumberIndexFieldTypeOptionsStep<LuceneBigIntegerIndexFieldTypeOptionsStep, BigInteger> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexFieldTypeDefaultsProvider defaultsProvider;

	private Integer decimalScale = null;

	LuceneBigIntegerIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext, IndexFieldTypeDefaultsProvider defaultsProvider) {
		super( buildContext, BigInteger.class );
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
	protected AbstractLuceneNumericFieldCodec<BigInteger, ?> createCodec(boolean resolvedProjectable,
			boolean resolvedSearchable, boolean resolvedSortable, boolean resolvedAggregable,
			BigInteger indexNullAsValue) {
		int resolvedDecimalScale = resolveDecimalScale();
		if ( resolvedDecimalScale > 0 ) {
			throw log.invalidDecimalScale( resolvedDecimalScale, getBuildContext().getEventContext() );
		}
		return new LuceneBigIntegerFieldCodec(
				resolvedProjectable, resolvedSearchable, resolvedSortable, resolvedAggregable, indexNullAsValue,
				resolvedDecimalScale
		);
	}

	private int resolveDecimalScale() {
		if ( decimalScale != null ) {
			return decimalScale;
		}
		if ( defaultsProvider.decimalScale() != null ) {
			return defaultsProvider.decimalScale();
		}

		throw log.nullDecimalScale( getBuildContext().getEventContext() );
	}
}

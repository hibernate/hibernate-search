/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigInteger;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchBigIntegerFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class ElasticsearchBigIntegerIndexFieldTypeOptionsStep
		extends AbstractElasticsearchNumericFieldTypeOptionsStep<ElasticsearchBigIntegerIndexFieldTypeOptionsStep, BigInteger>
		implements ScaledNumberIndexFieldTypeOptionsStep<ElasticsearchBigIntegerIndexFieldTypeOptionsStep, BigInteger> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexFieldTypeDefaultsProvider defaultsProvider;

	private Integer decimalScale = null;

	ElasticsearchBigIntegerIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext, IndexFieldTypeDefaultsProvider defaultsProvider) {
		super( buildContext, BigInteger.class, DataTypes.SCALED_FLOAT );
		this.defaultsProvider = defaultsProvider;
	}

	@Override
	public ElasticsearchBigIntegerIndexFieldTypeOptionsStep decimalScale(int decimalScale) {
		this.decimalScale = decimalScale;
		return thisAsS();
	}

	@Override
	protected ElasticsearchFieldCodec<BigInteger> completeCodec() {
		int resolvedDecimalScale = resolveDecimalScale();

		if ( resolvedDecimalScale > 0 ) {
			throw log.invalidDecimalScale( resolvedDecimalScale, buildContext.getEventContext() );
		}

		ElasticsearchBigIntegerFieldCodec codec = new ElasticsearchBigIntegerFieldCodec( resolvedDecimalScale );
		builder.mapping().setScalingFactor( codec.scalingFactor().doubleValue() );

		return codec;
	}

	@Override
	protected ElasticsearchBigIntegerIndexFieldTypeOptionsStep thisAsS() {
		return this;
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

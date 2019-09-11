/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchBigIntegerFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class ElasticsearchBigIntegerIndexFieldTypeOptionsStep
		extends AbstractElasticsearchScalarFieldTypeOptionsStep<ElasticsearchBigIntegerIndexFieldTypeOptionsStep, BigInteger>
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
	protected ElasticsearchFieldCodec<BigInteger> complete(PropertyMapping mapping) {
		int resolvedDecimalScale = resolveDecimalScale();

		if ( resolvedDecimalScale > 0 ) {
			throw log.invalidDecimalScale( resolvedDecimalScale, getBuildContext().getEventContext() );
		}

		BigDecimal scalingFactor = BigDecimal.TEN.pow( resolvedDecimalScale, new MathContext( 10, RoundingMode.HALF_UP ) );
		mapping.setScalingFactor( scalingFactor.doubleValue() );

		return new ElasticsearchBigIntegerFieldCodec( scalingFactor );
	}

	@Override
	protected ElasticsearchBigIntegerIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	private int resolveDecimalScale() {
		if ( decimalScale != null ) {
			return decimalScale;
		}
		if ( defaultsProvider.getDecimalScale() != null ) {
			return defaultsProvider.getDecimalScale();
		}

		throw log.nullDecimalScale( getBuildContext().getEventContext() );
	}
}

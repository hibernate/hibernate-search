/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchBigDecimalFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class ElasticsearchBigDecimalIndexFieldTypeOptionsStep
		extends AbstractElasticsearchNumericFieldTypeOptionsStep<ElasticsearchBigDecimalIndexFieldTypeOptionsStep, BigDecimal>
		implements ScaledNumberIndexFieldTypeOptionsStep<ElasticsearchBigDecimalIndexFieldTypeOptionsStep, BigDecimal> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexFieldTypeDefaultsProvider defaultsProvider;

	private Integer decimalScale = null;

	ElasticsearchBigDecimalIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext, IndexFieldTypeDefaultsProvider defaultsProvider) {
		super( buildContext, BigDecimal.class, DataTypes.SCALED_FLOAT );
		this.defaultsProvider = defaultsProvider;
	}

	@Override
	public ElasticsearchBigDecimalIndexFieldTypeOptionsStep decimalScale(int decimalScale) {
		this.decimalScale = decimalScale;
		return thisAsS();
	}

	@Override
	protected ElasticsearchFieldCodec<BigDecimal> completeCodec() {
		int resolvedDecimalScale = resolveDecimalScale();

		ElasticsearchBigDecimalFieldCodec codec = new ElasticsearchBigDecimalFieldCodec( resolvedDecimalScale );
		builder.mapping().setScalingFactor( codec.scalingFactor().doubleValue() );

		return codec;
	}

	@Override
	protected ElasticsearchBigDecimalIndexFieldTypeOptionsStep thisAsS() {
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

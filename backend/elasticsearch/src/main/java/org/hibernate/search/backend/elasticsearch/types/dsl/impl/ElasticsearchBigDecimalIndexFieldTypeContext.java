/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchBigDecimalFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class ElasticsearchBigDecimalIndexFieldTypeContext
		extends AbstractElasticsearchScalarFieldTypeContext<ElasticsearchBigDecimalIndexFieldTypeContext, BigDecimal>
		implements ScaledNumberIndexFieldTypeContext<ElasticsearchBigDecimalIndexFieldTypeContext, BigDecimal> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexFieldTypeDefaultsProvider defaultsProvider;

	private Integer decimalScale = null;

	ElasticsearchBigDecimalIndexFieldTypeContext(ElasticsearchIndexFieldTypeBuildContext buildContext, IndexFieldTypeDefaultsProvider defaultsProvider) {
		super( buildContext, BigDecimal.class, DataType.SCALED_FLOAT );
		this.defaultsProvider = defaultsProvider;
	}

	@Override
	public ElasticsearchBigDecimalIndexFieldTypeContext decimalScale(int decimalScale) {
		this.decimalScale = decimalScale;
		return thisAsS();
	}

	@Override
	protected ElasticsearchIndexFieldType<BigDecimal> toIndexFieldType(PropertyMapping mapping) {
		BigDecimal scalingFactor = BigDecimal.TEN.pow( resolveDecimalScale(), new MathContext( 10, RoundingMode.HALF_UP ) );
		mapping.setScalingFactor( scalingFactor.doubleValue() );

		ToDocumentFieldValueConverter<?, ? extends BigDecimal> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super BigDecimal, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();
		ElasticsearchBigDecimalFieldCodec codec = new ElasticsearchBigDecimalFieldCodec( scalingFactor );

		return new ElasticsearchIndexFieldType<>(
				codec,
				new ElasticsearchStandardFieldPredicateBuilderFactory<>( dslToIndexConverter , createToDocumentRawConverter(), codec ),
				new ElasticsearchStandardFieldSortBuilderFactory<>( resolvedSortable, dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new ElasticsearchStandardFieldProjectionBuilderFactory<>( resolvedProjectable, indexToProjectionConverter, createFromDocumentRawConverter(), codec ),
				mapping
		);
	}

	@Override
	protected ElasticsearchBigDecimalIndexFieldTypeContext thisAsS() {
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

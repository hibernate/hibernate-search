/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.aggregation.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractElasticsearchFieldAggregationBuilderFactory<F>
		implements ElasticsearchFieldAggregationBuilderFactory {
	protected static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final boolean aggregable;

	protected final DslConverter<?, ? extends F> toFieldValueConverter;
	protected final ProjectionConverter<? super F, ?> fromFieldValueConverter;

	protected final ElasticsearchFieldCodec<F> codec;

	public AbstractElasticsearchFieldAggregationBuilderFactory(
			boolean aggregable, DslConverter<?, ? extends F> toFieldValueConverter,
			ProjectionConverter<? super F, ?> fromFieldValueConverter,
			ElasticsearchFieldCodec<F> codec) {
		this.aggregable = aggregable;
		this.toFieldValueConverter = toFieldValueConverter;
		this.fromFieldValueConverter = fromFieldValueConverter;
		this.codec = codec;
	}

	@Override
	public boolean hasCompatibleCodec(ElasticsearchFieldAggregationBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractElasticsearchFieldAggregationBuilderFactory<?> castedOther =
				(AbstractElasticsearchFieldAggregationBuilderFactory<?>) other;
		return aggregable == castedOther.aggregable && codec.isCompatibleWith( castedOther.codec );
	}

	@Override
	public boolean hasCompatibleConverter(ElasticsearchFieldAggregationBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractElasticsearchFieldAggregationBuilderFactory<?> castedOther =
				(AbstractElasticsearchFieldAggregationBuilderFactory<?>) other;
		return toFieldValueConverter.isCompatibleWith( castedOther.toFieldValueConverter )
				&& fromFieldValueConverter.isCompatibleWith( castedOther.fromFieldValueConverter );
	}

	protected static void checkAggregable(String absoluteFieldPath, boolean aggregable) {
		if ( !aggregable ) {
			throw log.nonAggregableField( absoluteFieldPath,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}
	}
}

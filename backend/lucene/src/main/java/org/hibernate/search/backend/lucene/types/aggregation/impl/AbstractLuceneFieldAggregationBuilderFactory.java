/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractLuceneFieldAggregationBuilderFactory<F>
		implements LuceneFieldAggregationBuilderFactory {
	protected static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final boolean aggregable;

	protected final DslConverter<?, ? extends F> toFieldValueConverter;
	protected final ProjectionConverter<? super F, ?> fromFieldValueConverter;

	public AbstractLuceneFieldAggregationBuilderFactory(
			boolean aggregable, DslConverter<?, ? extends F> toFieldValueConverter,
			ProjectionConverter<? super F, ?> fromFieldValueConverter) {
		this.aggregable = aggregable;
		this.toFieldValueConverter = toFieldValueConverter;
		this.fromFieldValueConverter = fromFieldValueConverter;
	}

	@Override
	public boolean hasCompatibleCodec(LuceneFieldAggregationBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractLuceneFieldAggregationBuilderFactory<?> castedOther =
				(AbstractLuceneFieldAggregationBuilderFactory<?>) other;
		return aggregable == castedOther.aggregable && getCodec().isCompatibleWith( castedOther.getCodec() );
	}

	@Override
	public boolean hasCompatibleConverter(LuceneFieldAggregationBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractLuceneFieldAggregationBuilderFactory<?> castedOther =
				(AbstractLuceneFieldAggregationBuilderFactory<?>) other;
		return toFieldValueConverter.isCompatibleWith( castedOther.toFieldValueConverter )
				&& fromFieldValueConverter.isCompatibleWith( castedOther.fromFieldValueConverter );
	}

	protected abstract LuceneFieldCodec<F> getCodec();

	protected void checkAggregable(String absoluteFieldPath) {
		if ( !aggregable ) {
			throw log.nonAggregableField( absoluteFieldPath,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractLuceneFieldAggregationBuilderFactory<F>
		implements LuceneFieldAggregationBuilderFactory<F> {
	protected static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final boolean aggregable;

	public AbstractLuceneFieldAggregationBuilderFactory(boolean aggregable) {
		this.aggregable = aggregable;
	}

	@Override
	public boolean isAggregable() {
		return aggregable;
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldAggregationBuilderFactory<?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractLuceneFieldAggregationBuilderFactory<?> castedOther =
				(AbstractLuceneFieldAggregationBuilderFactory<?>) other;
		return aggregable == castedOther.aggregable && getCodec().isCompatibleWith( castedOther.getCodec() );
	}

	protected abstract LuceneFieldCodec<F> getCodec();

	protected void checkAggregable(LuceneSearchFieldContext<?> field) {
		if ( !aggregable ) {
			throw log.nonAggregableField( field.absolutePath(), field.eventContext() );
		}
	}

	protected <T> DslConverter<?, ? extends F> getToFieldValueConverter(LuceneSearchFieldContext<F> field,
			Class<T> expectedType, ValueConvert convert) {
		DslConverter<?, ? extends F> result = field.type().dslConverter( convert );
		if ( !result.isValidInputType( expectedType ) ) {
			throw log.invalidAggregationInvalidType( field.absolutePath(), expectedType, field.eventContext() );
		}
		return result;
	}

	@SuppressWarnings("unchecked") // We check the cast is legal by asking the converter
	protected <T> ProjectionConverter<? super F, ? extends T> getFromFieldValueConverter(
			LuceneSearchFieldContext<F> field, Class<T> expectedType, ValueConvert convert) {
		ProjectionConverter<? super F, ?> result = field.type().projectionConverter( convert );
		if ( !result.isConvertedTypeAssignableTo( expectedType ) ) {
			throw log.invalidAggregationInvalidType( field.absolutePath(), expectedType, field.eventContext() );
		}
		return (ProjectionConverter<? super F, ? extends T>) result;
	}
}

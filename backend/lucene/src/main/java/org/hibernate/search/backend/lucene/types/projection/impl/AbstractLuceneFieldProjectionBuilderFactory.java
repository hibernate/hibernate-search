/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.projection.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneFieldProjection;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractLuceneFieldProjectionBuilderFactory<F> implements LuceneFieldProjectionBuilderFactory<F> {
	protected static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final boolean projectable;

	protected final LuceneFieldCodec<F> codec;

	public AbstractLuceneFieldProjectionBuilderFactory(boolean projectable, LuceneFieldCodec<F> codec) {
		this.projectable = projectable;
		this.codec = codec;
	}

	@Override
	public boolean isProjectable() {
		return projectable;
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldProjectionBuilderFactory<?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractLuceneFieldProjectionBuilderFactory<?> castedOther =
				(AbstractLuceneFieldProjectionBuilderFactory<?>) other;
		return projectable == castedOther.projectable && codec.isCompatibleWith( castedOther.codec );
	}

	@Override
	@SuppressWarnings("unchecked") // We check the cast is legal by asking the converter
	public <T> FieldProjectionBuilder<T> createFieldValueProjectionBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field, Class<T> expectedType, ValueConvert convert) {
		checkProjectable( field );

		ProjectionConverter<? super F, ?> converter = field.type().projectionConverter( convert );
		if ( !converter.isConvertedTypeAssignableTo( expectedType ) ) {
			throw log.invalidProjectionInvalidType( field.absolutePath(), expectedType, field.eventContext() );
		}

		return (FieldProjectionBuilder<T>) new LuceneFieldProjection.Builder<>( searchContext, field, converter, codec );
	}

	protected void checkProjectable(LuceneSearchFieldContext<?> field) {
		if ( !projectable ) {
			throw log.nonProjectableField( field.absolutePath(), field.eventContext() );
		}
	}
}

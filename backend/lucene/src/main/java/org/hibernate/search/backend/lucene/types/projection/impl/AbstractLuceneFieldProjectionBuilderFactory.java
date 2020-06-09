/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneFieldProjectionBuilder;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractLuceneFieldProjectionBuilderFactory<F> implements LuceneFieldProjectionBuilderFactory {
	protected static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final boolean projectable;

	protected final ProjectionConverter<? super F, ?> converter;
	protected final ProjectionConverter<? super F, F> rawConverter;

	protected final LuceneFieldCodec<F> codec;

	public AbstractLuceneFieldProjectionBuilderFactory(
			boolean projectable, ProjectionConverter<? super F, ?> converter,
			ProjectionConverter<? super F, F> rawConverter, LuceneFieldCodec<F> codec) {
		this.projectable = projectable;
		this.converter = converter;
		this.rawConverter = rawConverter;
		this.codec = codec;
	}

	@Override
	public boolean isProjectable() {
		return projectable;
	}

	@Override
	@SuppressWarnings("unchecked") // We check the cast is legal by asking the converter
	public <T> FieldProjectionBuilder<T> createFieldValueProjectionBuilder(Set<String> indexNames,
			String absoluteFieldPath, String nestedDocumentPath, boolean multiValuedFieldInRoot,
			Class<T> expectedType, ValueConvert convert) {
		checkProjectable( absoluteFieldPath );

		ProjectionConverter<? super F, ?> requestConverter = getConverter( convert );
		if ( !requestConverter.isConvertedTypeAssignableTo( expectedType ) ) {
			throw log.invalidProjectionInvalidType( absoluteFieldPath, expectedType,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}

		return (FieldProjectionBuilder<T>) new LuceneFieldProjectionBuilder<>(
				indexNames, absoluteFieldPath, nestedDocumentPath, multiValuedFieldInRoot,
				requestConverter, codec
		);
	}

	@Override
	public boolean hasCompatibleCodec(LuceneFieldProjectionBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractLuceneFieldProjectionBuilderFactory<?> castedOther =
				(AbstractLuceneFieldProjectionBuilderFactory<?>) other;
		return projectable == castedOther.projectable && codec.isCompatibleWith( castedOther.codec );
	}

	@Override
	public boolean hasCompatibleConverter(LuceneFieldProjectionBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractLuceneFieldProjectionBuilderFactory<?> castedOther =
				(AbstractLuceneFieldProjectionBuilderFactory<?>) other;
		return converter.isCompatibleWith( castedOther.converter );
	}

	protected void checkProjectable(String absoluteFieldPath) {
		if ( !projectable ) {
			throw log.nonProjectableField( absoluteFieldPath,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}
	}

	protected ProjectionConverter<? super F, ?> getConverter(ValueConvert convert) {
		switch ( convert ) {
			case NO:
				return rawConverter;
			case YES:
			default:
				return converter;
		}
	}
}

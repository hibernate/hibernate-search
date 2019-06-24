/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.types.dsl.LuceneStandardIndexFieldTypeOptionsStep;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughFromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughToDocumentFieldValueConverter;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Contracts;

/**
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <F> The type of field values.
 */
abstract class AbstractLuceneStandardIndexFieldTypeOptionsStep<S extends AbstractLuceneStandardIndexFieldTypeOptionsStep<? extends S, F>, F>
		implements LuceneStandardIndexFieldTypeOptionsStep<S, F> {

	private final LuceneIndexFieldTypeBuildContext buildContext;
	private final Class<F> fieldType;

	private ToDocumentFieldValueConverter<?, ? extends F> dslToIndexConverter;
	private FromDocumentFieldValueConverter<? super F, ?> indexToProjectionConverter;
	protected Projectable projectable = Projectable.DEFAULT;
	protected Searchable searchable = Searchable.DEFAULT;
	protected F indexNullAsValue = null;

	AbstractLuceneStandardIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext, Class<F> fieldType) {
		this.buildContext = buildContext;
		this.fieldType = fieldType;
	}

	@Override
	public S dslConverter(ToDocumentFieldValueConverter<?, ? extends F> toIndexConverter) {
		Contracts.assertNotNull( toIndexConverter, "toIndexConverter" );
		this.dslToIndexConverter = toIndexConverter;
		return thisAsS();
	}

	@Override
	public S projectionConverter(FromDocumentFieldValueConverter<? super F, ?> fromIndexConverter) {
		Contracts.assertNotNull( fromIndexConverter, "fromIndexConverter" );
		this.indexToProjectionConverter = fromIndexConverter;
		return thisAsS();
	}

	@Override
	public S projectable(Projectable projectable) {
		this.projectable = projectable;
		return thisAsS();
	}

	@Override
	public S indexNullAs(F indexNullAs) {
		this.indexNullAsValue = indexNullAs;
		return thisAsS();
	}

	@Override
	public S searchable(Searchable searchable) {
		this.searchable = searchable;
		return thisAsS();
	}

	@Override
	public abstract LuceneIndexFieldType<F> toIndexFieldType();

	protected abstract S thisAsS();

	protected final LuceneIndexFieldTypeBuildContext getBuildContext() {
		return buildContext;
	}

	protected final ToDocumentFieldValueConverter<?, ? extends F> createDslToIndexConverter() {
		return dslToIndexConverter == null ? createToDocumentRawConverter() : dslToIndexConverter;
	}

	protected final ToDocumentFieldValueConverter<F, ? extends F> createToDocumentRawConverter() {
		return new PassThroughToDocumentFieldValueConverter<>( fieldType );
	}

	protected final FromDocumentFieldValueConverter<? super F, ?> createIndexToProjectionConverter() {
		return indexToProjectionConverter == null ? createFromDocumentRawConverter() : indexToProjectionConverter;
	}

	protected final FromDocumentFieldValueConverter<? super F, F> createFromDocumentRawConverter() {
		return new PassThroughFromDocumentFieldValueConverter<>( fieldType );
	}

	protected static boolean resolveDefault(Projectable projectable) {
		switch ( projectable ) {
			case DEFAULT:
			case NO:
				return false;
			case YES:
				return true;
			default:
				throw new AssertionFailure( "Unexpected value for Projectable: " + projectable );
		}
	}

	protected static boolean resolveDefault(Searchable searchable) {
		switch ( searchable ) {
			case DEFAULT:
			case YES:
				return true;
			case NO:
				return false;
			default:
				throw new AssertionFailure( "Unexpected value for Searchable: " + searchable );
		}
	}

	protected static boolean resolveDefault(Sortable sortable) {
		switch ( sortable ) {
			case DEFAULT:
			case NO:
				return false;
			case YES:
				return true;
			default:
				throw new AssertionFailure( "Unexpected value for Sortable: " + sortable );
		}
	}
}

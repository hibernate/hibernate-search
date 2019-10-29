/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.types.dsl.LuceneStandardIndexFieldTypeOptionsStep;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughFromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Contracts;

/**
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <F> The type of field values.
 */
abstract class AbstractLuceneStandardIndexFieldTypeOptionsStep<S extends AbstractLuceneStandardIndexFieldTypeOptionsStep<?, F>, F>
		implements LuceneStandardIndexFieldTypeOptionsStep<S, F> {

	private final LuceneIndexFieldTypeBuildContext buildContext;
	private final Class<F> fieldType;

	private DslConverter<?, ? extends F> dslConverter;
	private ProjectionConverter<? super F, ?> projectionConverter;
	protected Projectable projectable = Projectable.DEFAULT;
	protected Searchable searchable = Searchable.DEFAULT;
	protected Aggregable aggregable = Aggregable.DEFAULT;
	protected F indexNullAsValue = null;

	AbstractLuceneStandardIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext, Class<F> fieldType) {
		this.buildContext = buildContext;
		this.fieldType = fieldType;
	}

	@Override
	public <V> S dslConverter(Class<V> valueType, ToDocumentFieldValueConverter<V, ? extends F> toIndexConverter) {
		Contracts.assertNotNull( toIndexConverter, "toIndexConverter" );
		this.dslConverter = new DslConverter<>( valueType, toIndexConverter );
		return thisAsS();
	}

	@Override
	public <V> S projectionConverter(Class<V> valueType, FromDocumentFieldValueConverter<? super F, V> fromIndexConverter) {
		Contracts.assertNotNull( fromIndexConverter, "fromIndexConverter" );
		this.projectionConverter = new ProjectionConverter<>( valueType, fromIndexConverter );
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
	public S aggregable(Aggregable aggregable) {
		this.aggregable = aggregable;
		return thisAsS();
	}

	@Override
	public abstract LuceneIndexFieldType<F> toIndexFieldType();

	protected abstract S thisAsS();

	protected final LuceneIndexFieldTypeBuildContext getBuildContext() {
		return buildContext;
	}

	protected final DslConverter<?, ? extends F> createDslConverter() {
		return dslConverter == null ? createRawDslConverter() : dslConverter;
	}

	protected final DslConverter<F, ? extends F> createRawDslConverter() {
		return new DslConverter<>( fieldType, new PassThroughToDocumentFieldValueConverter<>() );
	}

	protected final ProjectionConverter<? super F, ?> createProjectionConverter() {
		return projectionConverter == null ? createRawProjectionConverter() : projectionConverter;
	}

	protected final ProjectionConverter<? super F, F> createRawProjectionConverter() {
		return new ProjectionConverter<>( fieldType, new PassThroughFromDocumentFieldValueConverter<>() );
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

	protected static boolean resolveDefault(Aggregable aggregable) {
		switch ( aggregable ) {
			case DEFAULT:
			case NO:
				return false;
			case YES:
				return true;
			default:
				throw new AssertionFailure( "Unexpected value for Aggregable: " + aggregable );
		}
	}
}

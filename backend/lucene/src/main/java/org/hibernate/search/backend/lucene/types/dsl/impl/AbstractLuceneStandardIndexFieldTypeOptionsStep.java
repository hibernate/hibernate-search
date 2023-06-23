/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.types.dsl.LuceneStandardIndexFieldTypeOptionsStep;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexValueFieldType;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.util.common.AssertionFailure;

/**
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <F> The type of field values.
 */
abstract class AbstractLuceneStandardIndexFieldTypeOptionsStep<
		S extends AbstractLuceneStandardIndexFieldTypeOptionsStep<?, F>,
		F>
		extends AbstractLuceneIndexFieldTypeOptionsStep<S, F>
		implements LuceneStandardIndexFieldTypeOptionsStep<S, F> {

	protected Projectable projectable = Projectable.DEFAULT;
	protected Searchable searchable = Searchable.DEFAULT;
	protected Aggregable aggregable = Aggregable.DEFAULT;
	protected F indexNullAsValue = null;

	AbstractLuceneStandardIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext, Class<F> valueType) {
		super( buildContext, valueType );
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
	public abstract LuceneIndexValueFieldType<F> toIndexFieldType();

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

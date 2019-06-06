/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.dsl;

import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;

/**
 * A context for specifying a type with capabilities that are considered "standard",
 * such as search or projection.
 *
 * @param <S> The concrete type of this context.
 * @param <F> The type of field values.
 */
public interface StandardIndexFieldTypeContext<S extends StandardIndexFieldTypeContext<? extends S, F>, F>
		extends IndexFieldTypeConverterContext<S, F>, IndexFieldTypeTerminalContext<F> {

	/**
	 * @param projectable Whether projections are enabled for this field.
	 * @return {@code this}, for method chaining.
	 * @see Projectable
	 */
	S projectable(Projectable projectable);

	/**
	 * @param sortable Whether this field should be sortable.
	 * @return {@code this}, for method chaining.
	 * @see Sortable
	 */
	S sortable(Sortable sortable);

	/**
	 * @param indexNullAs A value used instead of null values when indexing.
	 * @return {@code this}, for method chaining.
	 */
	S indexNullAs(F indexNullAs);

	/**
	 * @param searchable Whether this field should be searchable.
	 * @return {@code this}, for method chaining.
	 * @see Searchable
	 */
	S searchable(Searchable searchable);

}

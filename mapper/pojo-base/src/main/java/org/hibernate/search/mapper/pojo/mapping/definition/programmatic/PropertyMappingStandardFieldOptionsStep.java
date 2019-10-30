/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;

/**
 * The step in a property-to-index-field mapping where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface PropertyMappingStandardFieldOptionsStep<S extends PropertyMappingStandardFieldOptionsStep<?>>
		extends PropertyMappingFieldOptionsStep<S> {

	/**
	 * @param projectable Whether projections are enabled for this field.
	 * @return {@code this}, for method chaining.
	 * @see GenericField#projectable()
	 * @see Projectable
	 */
	S projectable(Projectable projectable);

	/**
	 * @param searchable Whether this field should be searchable.
	 * @return {@code this}, for method chaining.
	 * @see GenericField#searchable()
	 * @see Searchable
	 */
	S searchable(Searchable searchable);

	/**
	 * @param aggregable Whether aggregations are enabled for this field.
	 * @return {@code this}, for method chaining.
	 * @see GenericField#aggregable()
	 * @see Aggregable
	 */
	S aggregable(Aggregable aggregable);

}

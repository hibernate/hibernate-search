/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;

/**
 * A context to configure an index field mapped to a POJO property.
 *
 * @param <S> The "self" type, i.e. the type to return from methods.
 */
public interface PropertyNotFullTextFieldMappingContext<S extends PropertyNotFullTextFieldMappingContext<?>>
		extends PropertyFieldMappingContext<S> {

	/**
	 * @param sortable Whether this field should be sortable.
	 * @return {@code this}, for method chaining.
	 * @see GenericField#sortable()
	 * @see Sortable
	 */
	S sortable(Sortable sortable);

	/**
	 * @param indexNullAs A value used instead of null values when indexing.
	 * @return {@code this}, for method chaining.
	 * @see GenericField#indexNullAs()
	 */
	S indexNullAs(String indexNullAs);

}

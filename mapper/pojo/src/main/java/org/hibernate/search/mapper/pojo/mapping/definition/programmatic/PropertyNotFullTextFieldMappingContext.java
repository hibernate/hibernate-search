/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.backend.types.Sortable;

/**
 * @author Yoann Rodiere
 */
public interface PropertyNotFullTextFieldMappingContext<S extends PropertyNotFullTextFieldMappingContext<?>>
		extends PropertyFieldMappingContext<S> {

	S sortable(Sortable sortable);

	S indexNullAs(String indexNullAs);

}

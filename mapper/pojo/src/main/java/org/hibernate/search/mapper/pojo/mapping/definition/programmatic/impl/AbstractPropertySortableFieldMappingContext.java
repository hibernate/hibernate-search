/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.function.Function;

import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertySortableFieldMappingContext;


abstract class AbstractPropertySortableFieldMappingContext<S extends PropertySortableFieldMappingContext<?>, C extends StandardIndexSchemaFieldTypedContext<?, ?>>
		extends AbstractPropertyFieldMappingContext<S, C>
		implements PropertySortableFieldMappingContext<S> {

	AbstractPropertySortableFieldMappingContext(PropertyMappingContext parent, String relativeFieldName,
			Function<StandardIndexSchemaFieldTypedContext<?, ?>, C> contextConverter) {
		super( parent, relativeFieldName, contextConverter );
	}

	@Override
	public S sortable(Sortable sortable) {
		fieldModelContributor.add( c -> c.sortable( sortable ) );
		return thisAsS();
	}

}

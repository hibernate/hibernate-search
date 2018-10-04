/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertySortableFieldMappingContext;


abstract class PropertySortableFieldMappingContextImpl<S extends PropertySortableFieldMappingContext<?>>
		extends PropertyFieldMappingContextImpl<S>
		implements PropertySortableFieldMappingContext<S> {

	PropertySortableFieldMappingContextImpl(PropertyMappingContext parent, String relativeFieldName) {
		super( parent, relativeFieldName );
	}

	@Override
	public S sortable(Sortable sortable) {
		fieldModelContributor.add( c -> c.sortable( sortable ) );
		return thisAsS();
	}

}

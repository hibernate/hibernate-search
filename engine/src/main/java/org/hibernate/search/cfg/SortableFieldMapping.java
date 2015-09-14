/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg;

import java.util.Collections;
import java.util.Map;

/**
 * Configures a sortable field. Equivalent to {@code @SortableField}.
 *
 * @author Gunnar Morling
 */
public class SortableFieldMapping extends FieldMapping {

	public SortableFieldMapping(String fieldName, PropertyDescriptor property, EntityDescriptor entity, SearchMapping mapping) {
		super( property, entity, mapping );

		Map<String, Object> sortableField = Collections.<String, Object>singletonMap( "forField", fieldName );
		property.addSortableField( sortableField );
	}
}

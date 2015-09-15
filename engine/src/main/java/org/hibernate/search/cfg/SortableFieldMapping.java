/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.Collections;
import java.util.Map;

/**
 * Configures a sortable field. Equivalent to {@code @SortableField}.
 *
 * @author Gunnar Morling
 */
public class SortableFieldMapping {

	private final String fieldName;
	private final PropertyDescriptor property;
	private final EntityDescriptor entity;
	private final SearchMapping mapping;

	public SortableFieldMapping(String fieldName, PropertyDescriptor property, EntityDescriptor entity, SearchMapping mapping) {
		this.fieldName = fieldName;
		this.property = property;
		this.entity = entity;
		this.mapping = mapping;

		Map<String, Object> sortableField = Collections.<String, Object>singletonMap( "forField", fieldName );
		property.addSortableField( sortableField );
	}

	public FieldMapping field() {
		return new FieldMapping( property, entity, mapping );
	}

	public NumericFieldMapping numericField() {
		return new NumericFieldMapping( fieldName, property, entity, mapping );
	}

	public PropertyMapping property(String name, ElementType type) {
		return new PropertyMapping( name, type, entity, mapping );
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping( entityType, mapping );
	}
}

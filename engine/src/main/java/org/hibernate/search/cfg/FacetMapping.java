/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.annotations.FacetEncodingType;

/**
 * Configures a faceting field. Equivalent to {@code @Facet}.
 *
 * @author Yoann Rodiere
 */
public class FacetMapping {

	private final String fieldName;
	private final PropertyDescriptor property;
	private final EntityDescriptor entity;
	private final SearchMapping mapping;
	private final Map<String, Object> facet = new HashMap<String, Object>();

	public FacetMapping(String fieldName, PropertyDescriptor property, EntityDescriptor entity, SearchMapping mapping) {
		this.fieldName = fieldName;
		this.property = property;
		this.entity = entity;
		this.mapping = mapping;

		facet.put( "forField", fieldName );
		property.addFacet( facet );
	}

	public FacetMapping name(String fieldName) {
		facet.put( "name", fieldName );
		return this;
	}

	public FacetMapping encoding(FacetEncodingType encoding) {
		facet.put( "encoding", encoding );
		return this;
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

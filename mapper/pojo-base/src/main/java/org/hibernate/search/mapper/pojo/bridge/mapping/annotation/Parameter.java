/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.annotation;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingDocumentIdOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingIndexedStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

/**
 * A parameter, i.e. a name/value pair.
 */
public @interface Parameter {

	/**
	 * @return The name of the parameter.
	 * Must be unique in a given set of parameters.
	 */
	String name();

	/**
	 * @return The value of the parameter.
	 *
	 * @see PropertyMappingFieldOptionsStep#valueBinder(ValueBinder, Map)
	 * @see PropertyMappingStep#binder(PropertyBinder, Map)
	 * @see TypeMappingStep#binder(TypeBinder, Map)
	 * @see PropertyMappingDocumentIdOptionsStep#identifierBinder(IdentifierBinder, Map)
	 * @see TypeMappingIndexedStep#routingBinder(RoutingBinder, Map)
	 * @see PropertyMappingStep#marker(MarkerBinder, Map)
	 */
	String value();

}

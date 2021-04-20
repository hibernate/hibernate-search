/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.annotation;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

/**
 * A parameter that can be statically passed to a given binder,
 * so that it can be used by its bridge.
 */
public @interface Parameter {

	/**
	 * @return The name of the parameter.
	 * Must be unique for the bridge is used in.
	 */
	String name();

	/**
	 * @return The value of the parameter.
	 * Using annotation APIs is possible to pass only {@link String} values.
	 * The programmatic APIs allow to pass any {@link Object} value.
	 *
	 * @see {@link PropertyMappingFieldOptionsStep#valueBinder(ValueBinder, Map)}.
	 * @see {@link PropertyMappingStep#binder(PropertyBinder, Map)}.
	 * @see {@link TypeMappingStep#binder(TypeBinder, Map)}.
	 */
	String value();

}

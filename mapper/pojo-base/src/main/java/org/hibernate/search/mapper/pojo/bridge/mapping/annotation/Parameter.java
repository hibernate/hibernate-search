/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.annotation;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFieldOptionsStep;

/**
 * A parameter that can be statically passed to a given {@link ValueBinder},
 * so that it can be used by its {@link ValueBridge}.
 */
public @interface Parameter {

	/**
	 * @return The name of the parameter.
	 * Must be unique for the {@link ValueBridge} is used in.
	 */
	String name();

	/**
	 * @return The value of the parameter.
	 * Using annotation APIs is possible to pass only {@link String} values.
	 * The programmatic APIs allow to pass any {@link Object} value.
	 * See {@link PropertyMappingFieldOptionsStep#valueBinder(ValueBinder, Map)}.
	 */
	String value();

}

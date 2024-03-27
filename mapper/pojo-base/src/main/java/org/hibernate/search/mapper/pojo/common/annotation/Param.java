/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.common.annotation;

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
 * A param, i.e. a name/value pair.
 */
public @interface Param {

	/**
	 * @return The name of the param.
	 * Must be unique in a given set of params.
	 */
	String name();

	/**
	 * @return The value of the param.
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

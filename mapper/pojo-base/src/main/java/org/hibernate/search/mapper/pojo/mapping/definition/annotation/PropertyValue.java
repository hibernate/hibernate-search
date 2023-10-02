/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;

/**
 * A reference to a POJO property using its name,
 * and to one or more target value(s) in that property using a {@link ContainerExtraction}.
 */
@Documented
@Target({ }) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyValue {

	/**
	 * @return The name of the referenced property.
	 */
	String propertyName();

	/**
	 * @return A definition of container extractors to be applied to the property,
	 * allowing the binding of a value bridge to container elements.
	 * This is useful when the property is of container type,
	 * for example a {@code Map<TypeA, TypeB>}:
	 * defining the extraction as {@code @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)}
	 * allows referencing map keys instead of map values.
	 * By default, Hibernate Search will try to apply a set of extractors for common container types.
	 * @see ContainerExtraction
	 */
	ContainerExtraction extraction() default @ContainerExtraction;

}

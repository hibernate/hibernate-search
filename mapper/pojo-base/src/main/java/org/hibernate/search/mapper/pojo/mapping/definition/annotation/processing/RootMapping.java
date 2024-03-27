/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation for annotations that Hibernate Search should attempt to discover on bootstrap,
 * in order to automatically detect mapped types,
 * without application developers having to explicitly list mapped types.
 * <p>
 * This is useful in particular for mapping annotations that are not related to the index mapping,
 * where annotated types may not be referenced from the entity types used as the starting point for the index mapping.
 * <p>
 * Note that discovery of such annotations relies on Hibernate Search scanning the application JARs,
 * which may require additional setup.
 *
 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity
 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor
 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingConfigurationContext
 */
@Documented
@Target(value = ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RootMapping {
}

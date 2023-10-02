/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.loading.mapping.annotation.EntityLoadingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.RootMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.SearchEntityProcessor;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Given a type, marks this type as an entity type.
 * <p>
 * WARNING: this is unnecessary when using the Hibernate ORM integration,
 * which contributes this information automatically,
 * and is in fact unsupported with the Hibernate ORM integration.
 * See <a href="https://hibernate.atlassian.net/browse/HSEARCH-5076">HSEARCH-5076</a>
 * to track progress on allowing the use of `@SearchEntity` in the Hibernate ORM integration
 * to map non-ORM entities.
 */
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@RootMapping
@TypeMapping(processor = @TypeMappingAnnotationProcessorRef(type = SearchEntityProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
@Incubating
public @interface SearchEntity {

	/**
	 * @return The name of the entity type.
	 * Defaults to the JPA entity name for the Hibernate ORM mapper,
	 * or failing that to the {@link Class#getSimpleName() simple class name}.
	 */
	String name() default "";

	/**
	 * @return The binder for loading of entities of this type.
	 * <p>
	 * Note: this is unnecessary when using the Hibernate ORM mapper,
	 * which contributes this information automatically.
	 */
	EntityLoadingBinderRef loadingBinder() default @EntityLoadingBinderRef;

}

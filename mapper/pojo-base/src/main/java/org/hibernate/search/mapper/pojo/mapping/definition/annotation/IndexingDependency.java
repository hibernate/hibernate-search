/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.IndexingDependencyProcessor;

/**
 * Given a property, defines how a dependency of the indexing process to this property
 * should affect its reindexing.
 * <p>
 * This annotation is generally not needed, as the default behavior is to consider all properties
 * that are actually used in the indexing process as dependencies that trigger reindexing when they are updated.
 * <p>
 * However, some tuning may be required for some properties:
 * <ul>
 *     <li>Some properties may be updated very frequently
 *     and/or trigger reindexing to other entities that are very expensive to load in memory.
 *     In that case, it may be a good idea to tell Hibernate Search to ignore updates to those properties
 *     using {@link #reindexOnUpdate()}.
 *     The index will be slightly out-of-sync whenever the property is modified,
 *     but this can be solved by triggering reindexing manually, for example every night.
 *     </li>
 *     <li>Some properties may be computed dynamically based on other properties, instead of being stored.
 *     In that case, the mapper may not be able to detect changes to the computed property directly.
 *     Thus Hibernate Search needs to know which other properties are used when generating the value of this property,
 *     which can be configured using {@link #derivedFrom()}.
 *     </li>
 * </ul>
 * <p>
 * This annotation may be applied multiple times to the same property with different {@link #extraction() extractions},
 * to configure differently the dependency to different container elements.
 * For example with a property of type {@code Map<Entity1, Entity2>},
 * one can have a dependency to {@code Entity1} (map keys) or {@code Entity2} (map values),
 * and each may require a different configuration.
 */
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(IndexingDependency.List.class)
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = IndexingDependencyProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface IndexingDependency {

	/**
	 * @return How indexed entities using the annotated property should be reindexed when the value,
	 * or any nested value, is updated.
	 * This setting is only effective for values that are actually used when indexing
	 * (values used in field definitions, in bridges, ...).
	 */
	ReindexOnUpdate reindexOnUpdate() default ReindexOnUpdate.DEFAULT;

	/**
	 * @return Paths to other values that are used to generate the value of this property.
	 * Paths are relative to the parent type of the annotated property.
	 * This is useful mainly for getters that are not simply bound to class field,
	 * but rather compute a value based on other properties:
	 * it allows Hibernate Search to know that whenever these other properties are changed,
	 * this property may change too and thus should be reindexed.
	 */
	ObjectPath[] derivedFrom() default { };

	/**
	 * @return A definition of container extractors to be applied to the property,
	 * allowing the definition of the indexing dependencies for container elements.
	 * This is useful when the property is of container type,
	 * for example a {@code Map<TypeA, TypeB>}:
	 * defining the extraction as {@code @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)}
	 * allows referencing map keys instead of map values.
	 * By default, Hibernate Search will try to apply a set of extractors for common container types.
	 * @see ContainerExtraction
	 */
	ContainerExtraction extraction() default @ContainerExtraction;

	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		IndexingDependency[] value();
	}

}

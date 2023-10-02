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

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.GenericFieldProcessor;

/**
 * Maps an entity property to a field in the index.
 * <p>
 * This is a generic annotation that will work for any standard field type supported by the backend:
 * {@link String}, {@link Integer}, {@link java.time.LocalDate}, ...
 * <p>
 * Note that this annotation, being generic, does not offer configuration options
 * that are specific to only some types of fields.
 * Use more specific annotations if you want that kind of configuration.
 * For example, to define a tokenized (multi-word) text field, use {@link FullTextField}.
 * To define a non-tokenized (single-word), but normalized (lowercased, ...) text field, use {@link KeywordField}.
 */
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(GenericField.List.class)
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = GenericFieldProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface GenericField {

	/**
	 * @return The name of the index field.
	 */
	String name() default "";

	/**
	 * @return Whether projections are enabled for this field.
	 */
	Projectable projectable() default Projectable.DEFAULT;

	/**
	 * @return Whether this field should be sortable.
	 */
	Sortable sortable() default Sortable.DEFAULT;

	/**
	 * @return Whether this field should be searchable.
	 * @see Searchable
	 */
	Searchable searchable() default Searchable.DEFAULT;

	/**
	 * @return Whether aggregations are enabled for this field.
	 * @see Aggregable
	 */
	Aggregable aggregable() default Aggregable.DEFAULT;

	/**
	 * @return A value used instead of null values when indexing.
	 */
	String indexNullAs() default AnnotationDefaultValues.DO_NOT_INDEX_NULL;

	/**
	 * @return A reference to the value bridge to use for this field.
	 * Must not be set if {@link #valueBinder()} is set.
	 * @see ValueBridgeRef
	 */
	ValueBridgeRef valueBridge() default @ValueBridgeRef;

	/**
	 * @return A reference to the value binder to use for this field.
	 * Must not be set if {@link #valueBridge()} is set.
	 * @see ValueBinderRef
	 */
	ValueBinderRef valueBinder() default @ValueBinderRef;

	/**
	 * @return A definition of container extractors to be applied to the property,
	 * allowing the binding of a value bridge to container elements.
	 * This is useful when the property is of container type,
	 * for example a {@code Map<TypeA, TypeB>}:
	 * defining the extraction as {@code @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)}
	 * allows binding the field to the map keys instead of the map values.
	 * By default, Hibernate Search will try to apply a set of extractors for common container types.
	 * @see ContainerExtraction
	 */
	ContainerExtraction extraction() default @ContainerExtraction;

	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		GenericField[] value();
	}

}

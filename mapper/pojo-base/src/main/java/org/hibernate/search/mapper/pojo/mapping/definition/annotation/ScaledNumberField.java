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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.ScaledNumberFieldProcessor;

/**
 * Maps a property to a scaled number field in the index,
 * i.e. a numeric field for integer or floating-point values
 * that require a higher precision than doubles
 * but always have roughly the same scale.
 * <p>
 * Useful for {@link java.math.BigDecimal} and {@link java.math.BigInteger} in particular.
 *
 * @see #decimalScale()
 */
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ScaledNumberField.List.class)
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = ScaledNumberFieldProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface ScaledNumberField {

	/**
	 * @return The name of the index field.
	 */
	String name() default "";

	/**
	 * @return How the scale of values should be adjusted before indexing as a fixed-precision integer.
	 * A positive {@code decimalScale} will shift the decimal point to the right before rounding to the nearest integer and indexing,
	 * effectively retaining that many digits after the decimal place in the index.
	 * Since numbers are indexed with a fixed number of bits,
	 * this increase in precision also means that the maximum value that can be indexed will be smaller.
	 * A negative {@code decimalScale} will shift the decimal point to the left before rounding to the nearest integer and indexing,
	 * effectively setting that many of the smaller digits to zero in the index.
	 * Since numbers are indexed with a fixed number of bits,
	 * this decrease in precision also means that the maximum value that can be indexed will be larger.
	 */
	int decimalScale() default AnnotationDefaultValues.DEFAULT_DECIMAL_SCALE;

	/**
	 * @return Whether projections are enabled for this field.
	 * @see GenericField#projectable()
	 * @see Projectable
	 */
	Projectable projectable() default Projectable.DEFAULT;

	/**
	 * @return Whether this field should be sortable.
	 * @see GenericField#sortable()
	 * @see Sortable
	 */
	Sortable sortable() default Sortable.DEFAULT;

	/**
	 * @return Whether this field should be searchable.
	 * @see GenericField#searchable()
	 * @see Searchable
	 */
	Searchable searchable() default Searchable.DEFAULT;

	/**
	 * @return Whether aggregations are enabled for this field.
	 * @see GenericField#aggregable()
	 * @see Aggregable
	 */
	Aggregable aggregable() default Aggregable.DEFAULT;

	/**
	 * @return A value used instead of null values when indexing.
	 * @see GenericField#indexNullAs()
	 */
	String indexNullAs() default AnnotationDefaultValues.DO_NOT_INDEX_NULL;

	/**
	 * @return A reference to the value bridge to use for this field.
	 * Must not be set if {@link #valueBinder()} is set.
	 * @see GenericField#valueBridge()
	 * @see ValueBridgeRef
	 */
	ValueBridgeRef valueBridge() default @ValueBridgeRef;

	/**
	 * @return A reference to the value binder to use for this field.
	 * Must not be set if {@link #valueBridge()} is set.
	 * @see GenericField#valueBinder()
	 * @see ValueBinderRef
	 */
	ValueBinderRef valueBinder() default @ValueBinderRef;

	/**
	 * @return A definition of container extractors to be applied to the property
	 * allowing the binding of a value bridge to container elements.
	 * By default, Hibernate Search will try to apply a set of extractors for common container types.
	 * @see GenericField#extraction()
	 */
	ContainerExtraction extraction() default @ContainerExtraction;

	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		ScaledNumberField[] value();
	}
}

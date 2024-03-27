/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.binding;

import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeConverterStep;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeOptionsStep;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.model.PojoModelValue;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The context provided to the {@link ValueBinder#bind(ValueBindingContext)} method.
 *
 * @param <V> The type of values on the POJO side of the bridge.
 */
public interface ValueBindingContext<V> extends BindingContext {

	/**
	 * Sets the bridge implementing the value/index binding.
	 *
	 * @param expectedValueType The type of values expected by the given bridge.
	 * Hibernate Search will check that these expectations are met, and throw an exception if they are not.
	 * @param bridge The bridge to use at runtime to convert between the POJO property value and the index field value.
	 * @param <V2> The type of values expected by the given bridge.
	 * @param <F> The type of index field values, on the index side of the bridge.
	 */
	<V2, F> void bridge(Class<V2> expectedValueType, ValueBridge<V2, F> bridge);

	/**
	 * Sets the bridge implementing the value/index binding.
	 *
	 * @param expectedValueType The type of values expected by the given bridge.
	 * Hibernate Search will check that these expectations are met, and throw an exception if they are not.
	 * @param bridge The bridge to use at runtime to convert between the POJO property value and the index field value.
	 * @param fieldTypeOptionsStep The result of calling {@code context.getTypeFactory()}
	 * and setting the expectations regarding the index field type
	 * (for instance {@code return context.getTypeFactory().asString()}).
	 * {@code null} to let Hibernate Search derive the expectations
	 * from the {@code ValueBridge}'s generic type parameters.
	 * <p>
	 * Note the {@link IndexFieldTypeConverterStep#dslConverter(Class, ToDocumentValueConverter) DSL converter}
	 * and {@link IndexFieldTypeConverterStep#projectionConverter(Class, FromDocumentValueConverter) projection converter}
	 * will be ignored, since they are already implemented by the value bridge itself
	 * through its {@link ValueBridge#toIndexedValue(Object, ValueBridgeToIndexedValueContext)}
	 * and {@link ValueBridge#fromIndexedValue(Object, ValueBridgeFromIndexedValueContext)} methods.
	 * @param <V2> The type of values expected by the given bridge.
	 * @param <F> The type of index field values, on the index side of the bridge.
	 */
	<V2, F> void bridge(Class<V2> expectedValueType, ValueBridge<V2, F> bridge,
			IndexFieldTypeOptionsStep<?, F> fieldTypeOptionsStep);

	/**
	 * Sets the bridge implementing the value/index binding.
	 *
	 * @param expectedValueType The type of values expected by the given bridge.
	 * Hibernate Search will check that these expectations are met, and throw an exception if they are not.
	 * @param bridgeHolder A {@link BeanHolder} containing
	 * the bridge to use at runtime to convert between the POJO property value and the index field value.
	 * Use {@link BeanHolder#of(Object)} if you don't need any particular closing behavior.
	 * @param fieldTypeOptionsStep The result of calling {@code context.getTypeFactory()}
	 * and setting the expectations regarding the index field type
	 * (for instance {@code return context.getTypeFactory().asString()}).
	 * {@code null} to let Hibernate Search derive the expectations
	 * from the {@code ValueBridge}'s generic type parameters.
	 * <p>
	 * Note the {@link IndexFieldTypeConverterStep#dslConverter(Class, ToDocumentValueConverter) DSL converter}
	 * and {@link IndexFieldTypeConverterStep#projectionConverter(Class, FromDocumentValueConverter) projection converter}
	 * will be ignored, since they are already implemented by the value bridge itself
	 * through its {@link ValueBridge#toIndexedValue(Object, ValueBridgeToIndexedValueContext)}
	 * and {@link ValueBridge#fromIndexedValue(Object, ValueBridgeFromIndexedValueContext)} methods.
	 * @param <V2> The type of values expected by the given bridge.
	 * @param <F> The type of index field values, on the index side of the bridge.
	 */
	<V2, F> void bridge(Class<V2> expectedValueType, BeanHolder<? extends ValueBridge<V2, F>> bridgeHolder,
			IndexFieldTypeOptionsStep<?, F> fieldTypeOptionsStep);

	/**
	 * @return An entry point allowing to inspect the type of values that will be passed to this bridge.
	 */
	@Incubating
	PojoModelValue<V> bridgedElement();

	/**
	 * @return An entry point allowing to define a new field type.
	 */
	IndexFieldTypeFactory typeFactory();

}

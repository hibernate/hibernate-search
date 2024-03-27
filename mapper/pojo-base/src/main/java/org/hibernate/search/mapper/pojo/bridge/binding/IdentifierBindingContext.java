/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.binding;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.model.PojoModelValue;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The context provided to the {@link IdentifierBinder#bind(IdentifierBindingContext)} method.
 *
 * @param <I> The type of identifiers on the POJO side of the bridge.
 */
public interface IdentifierBindingContext<I> extends BindingContext {

	/**
	 * Sets the bridge implementing the value/index binding.
	 *
	 * @param expectedIdentifierType The type of identifiers expected by the given bridge.
	 * Hibernate Search will check that these expectations are met, and throw an exception if they are not.
	 * @param bridge The bridge to use at runtime to convert between the POJO identifier and the document identifier.
	 * @param <I2> The type of identifiers expected by the given bridge.
	 */
	<I2> void bridge(Class<I2> expectedIdentifierType, IdentifierBridge<I2> bridge);

	/**
	 * Sets the bridge implementing the value/index binding.
	 *
	 * @param expectedIdentifierType The type of values expected by the given bridge.
	 * Hibernate Search will check that these expectations are met, and throw an exception if they are not.
	 * @param bridgeHolder A {@link BeanHolder} containing
	 * the bridge to use at runtime to convert between the POJO property value and the index field value.
	 * Use {@link BeanHolder#of(Object)} if you don't need any particular closing behavior.
	 * @param <I2> The type of identifiers expected by the given bridge.
	 */
	<I2> void bridge(Class<I2> expectedIdentifierType, BeanHolder<? extends IdentifierBridge<I2>> bridgeHolder);

	/**
	 * @return An entry point allowing to inspect the type of values that will be passed to this bridge.
	 */
	@Incubating
	PojoModelValue<I> bridgedElement();

}

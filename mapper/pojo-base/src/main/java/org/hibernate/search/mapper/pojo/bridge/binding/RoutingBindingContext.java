/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.binding;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.mapper.pojo.model.dependency.PojoRoutingIndexingDependencyConfigurationContext;
import org.hibernate.search.util.common.annotation.Incubating;

public interface RoutingBindingContext extends BindingContext {

	/**
	 * Sets the object responsible for routing indexed entities to the correct index/shard.
	 *
	 * @param expectedType The expected entity type.
	 * @param bridge The bridge to use when indexing.
	 * @param <E> The expected entity type.
	 */
	<E> void bridge(Class<E> expectedType, RoutingBridge<E> bridge);

	/**
	 * Sets the object responsible for routing indexed entities to the correct index/shard.
	 *
	 * @param expectedType The expected entity type.
	 * @param bridgeHolder A {@link BeanHolder} containing the bridge to use when indexing.
	 * @param <E> The expected entity type.
	 */
	<E> void bridge(Class<E> expectedType, BeanHolder<? extends RoutingBridge<E>> bridgeHolder);

	/**
	 * @return An entry point allowing to declare expectations and retrieve accessors to the bridged POJO type
	 * (i.e. the indexed type).
	 */
	@Incubating
	PojoModelType bridgedElement();

	/**
	 * @return An entry point allowing to declare the parts of the entity graph that the bridge will depend on.
	 */
	PojoRoutingIndexingDependencyConfigurationContext dependencies();

}

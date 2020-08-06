/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.mapper.pojo.model.dependency.PojoTypeIndexingDependencyConfigurationContext;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The context provided to the {@link org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder#bind(RoutingKeyBindingContext)} method.
 *
 * @deprecated This is only useful in {@link org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder},
 * which is deprecated.
 */
@Deprecated
public interface RoutingKeyBindingContext extends BindingContext {

	/**
	 * Sets the bridge implementing the type/routing key binding.
	 *
	 * @param bridge The bridge to use at runtime to generate a routing key.
	 */
	// FIXME also require the caller to pass the expected raw type here, and validate it.
	//  We'll need to add generic type parameters to TypeBridge, however.
	void bridge(org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge bridge);

	/**
	 * Sets the bridge implementing the type/routing key binding.
	 *
	 * @param bridge The bridge to use at runtime to generate a routing key.
	 * @deprecated Use {@link #bridge(org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge)} instead.
	 */
	@Deprecated
	default void setBridge(org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge bridge) {
		bridge( bridge );
	}

	/**
	 * Sets the bridge implementing the type/routing key binding.
	 *
	 * @param bridgeHolder A {@link BeanHolder} containing
	 * the bridge to use at runtime to generate a routing key.
	 * Use {@link BeanHolder#of(Object)} if you don't need any particular closing behavior.
	 */
	// FIXME also require the caller to pass the expected raw type here, and validate it.
	//  We'll need to add generic type parameters to TypeBridge, however.
	void bridge(BeanHolder<? extends org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge> bridgeHolder);

	/**
	 * Sets the bridge implementing the type/routing key binding.
	 *
	 * @param bridgeHolder A {@link BeanHolder} containing
	 * the bridge to use at runtime to generate a routing key.
	 * Use {@link BeanHolder#of(Object)} if you don't need any particular closing behavior.
	 * @deprecated Use {@link #bridge(BeanHolder)} instead.
	 */
	@Deprecated
	default void setBridge(BeanHolder<? extends org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge> bridgeHolder) {
		bridge( bridgeHolder );
	}

	/**
	 * @return An entry point allowing to declare expectations and retrieve accessors to the bridged POJO type.
	 */
	@Incubating
	PojoModelType bridgedElement();

	/**
	 * @return An entry point allowing to declare expectations and retrieve accessors to the bridged POJO type.
	 * @deprecated Use {@link #bridgedElement()} instead.
	 */
	@Deprecated
	default PojoModelType getBridgedElement() {
		return bridgedElement();
	}

	/**
	 * @return An entry point allowing to declare the parts of the entity graph that this bridge will depend on.
	 */
	PojoTypeIndexingDependencyConfigurationContext dependencies();

	/**
	 * @return An entry point allowing to declare the parts of the entity graph that this bridge will depend on.
	 * @deprecated Use {@link #dependencies()} instead.
	 */
	@Deprecated
	default PojoTypeIndexingDependencyConfigurationContext getDependencies() {
		return dependencies();
	}

}

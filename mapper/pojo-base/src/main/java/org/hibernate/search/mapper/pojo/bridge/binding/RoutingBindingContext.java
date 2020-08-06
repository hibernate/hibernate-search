/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
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

}

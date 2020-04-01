/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding;

import java.util.Map;
import org.hibernate.search.engine.environment.bean.BeanHolder;

/**
 * The context provided to the {@link FilterFactoryBinder#bind(FilterBindingContext)} method.
 *
 * @param <T> The type of factory on the POJO side of the factory.
 */
public interface FilterBindingContext<T> extends BindingContext {

	void setFactory(T factory, Map<String, Object> params);

	void setFactory(BeanHolder<T> factoryHolder, Map<String, Object> params);

}

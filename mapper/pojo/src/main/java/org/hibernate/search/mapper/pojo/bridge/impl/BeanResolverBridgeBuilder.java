/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.impl;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanProvider;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuildContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;

public class BeanResolverBridgeBuilder<T> implements BridgeBuilder<T> {

	private final Class<T> expectedType;

	private final BeanReference beanReference;

	public BeanResolverBridgeBuilder(Class<T> expectedType, BeanReference beanReference) {
		this.expectedType = expectedType;
		this.beanReference = beanReference;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + beanReference + "]";
	}

	@Override
	public T build(BridgeBuildContext buildContext) {
		BeanProvider beanProvider = buildContext.getBeanProvider();
		return beanProvider.getBean( beanReference, expectedType );
	}
}

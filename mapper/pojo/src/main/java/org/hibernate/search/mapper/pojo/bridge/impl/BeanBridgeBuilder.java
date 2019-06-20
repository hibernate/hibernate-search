/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.impl;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanProvider;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuildContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuilder;

/**
 * A bridge builder that simply retrieves the bridge as a bean from the bean provider.
 *
 * @param <B> The type of bridges returned by this builder.
 */
public final class BeanBridgeBuilder<B> implements BridgeBuilder<B> {

	private final BeanReference<B> beanReference;

	public BeanBridgeBuilder(BeanReference<B> beanReference) {
		this.beanReference = beanReference;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + beanReference + "]";
	}

	@Override
	public BeanHolder<B> build(BridgeBuildContext buildContext) {
		BeanProvider beanProvider = buildContext.getBeanProvider();
		return beanReference.getBean( beanProvider );
	}
}

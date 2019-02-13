/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.impl;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanProvider;
import org.hibernate.search.engine.environment.bean.spi.BeanResolver;
import org.hibernate.search.util.common.impl.Contracts;

/**
 * A BeanProvider that ignores the explicitly configured beans.
 * Used in the ConfiguredBeanProvider constructor to retrieve bean configurers.
 */
final class BeanResolverOnlyBeanProvider implements BeanProvider {
	private final BeanResolver beanResolver;

	BeanResolverOnlyBeanProvider(BeanResolver beanResolver) {
		this.beanResolver = beanResolver;
	}

	@Override
	public <T> BeanHolder<T> getBean(Class<T> typeReference) {
		Contracts.assertNotNull( typeReference, "typeReference" );
		return beanResolver.resolve( typeReference );
	}

	@Override
	public <T> BeanHolder<T> getBean(Class<T> typeReference, String nameReference) {
		Contracts.assertNotNull( typeReference, "typeReference" );
		Contracts.assertNotNullNorEmpty( nameReference, "nameReference" );
		return beanResolver.resolve( typeReference, nameReference );
	}
}

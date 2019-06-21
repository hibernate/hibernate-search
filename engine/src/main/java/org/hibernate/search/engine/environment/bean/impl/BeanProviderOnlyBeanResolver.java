/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.impl;

import java.util.List;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Contracts;

/**
 * A BeanResolver that ignores the explicitly configured beans.
 * Used in the ConfiguredBeanResolver constructor to retrieve bean configurers.
 */
final class BeanProviderOnlyBeanResolver implements BeanResolver {
	private final BeanProvider beanProvider;

	BeanProviderOnlyBeanResolver(BeanProvider beanProvider) {
		this.beanProvider = beanProvider;
	}

	@Override
	public <T> BeanHolder<T> resolve(Class<T> typeReference) {
		Contracts.assertNotNull( typeReference, "typeReference" );
		return beanProvider.getBean( typeReference );
	}

	@Override
	public <T> BeanHolder<T> resolve(Class<T> typeReference, String nameReference) {
		Contracts.assertNotNull( typeReference, "typeReference" );
		Contracts.assertNotNullNorEmpty( nameReference, "nameReference" );
		return beanProvider.getBean( typeReference, nameReference );
	}

	@Override
	public <T> BeanHolder<List<T>> resolveRole(Class<T> role) {
		throw new AssertionFailure( "Unexpected call to resolveRole before roles are even defined." );
	}
}

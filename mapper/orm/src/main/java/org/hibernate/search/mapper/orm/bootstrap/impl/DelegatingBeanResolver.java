/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.bootstrap.impl;

import java.util.Objects;

import org.hibernate.search.engine.common.spi.BeanReference;
import org.hibernate.search.engine.common.spi.BeanResolver;

/**
 * An adapter from the {@link org.hibernate.search.mapper.orm.spi.BeanResolver} interface
 * to {@link org.hibernate.search.engine.common.spi.BeanResolver}.
 */
final class DelegatingBeanResolver implements BeanResolver {

	private final org.hibernate.search.mapper.orm.spi.BeanResolver hibernateOrmBeanResolver;

	public DelegatingBeanResolver(org.hibernate.search.mapper.orm.spi.BeanResolver hibernateOrmBeanResolver) {
		Objects.requireNonNull( hibernateOrmBeanResolver );
		this.hibernateOrmBeanResolver = hibernateOrmBeanResolver;
	}

	@Override
	public <T> T resolve(Class<?> reference, Class<T> expectedClass) {
		return hibernateOrmBeanResolver.resolve( reference, expectedClass );
	}

	@Override
	public <T> T resolve(String implementationName, Class<T> expectedClass) {
		return hibernateOrmBeanResolver.resolve( implementationName, expectedClass );
	}

	@Override
	public <T> T resolve(BeanReference reference, Class<T> expectedClass) {
		return hibernateOrmBeanResolver.resolve( reference, expectedClass );
	}

}

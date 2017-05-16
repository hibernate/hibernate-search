/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg.impl;

import java.util.Objects;

import org.hibernate.search.engine.service.beanresolver.spi.BeanResolver;

/**
 * An adapter from the {@link org.hibernate.search.hcore.spi.BeanResolver} interface
 * to {@link org.hibernate.search.engine.service.beanresolver.spi.BeanResolver}.
 */
final class DelegatingBeanResolver implements BeanResolver {

	private final org.hibernate.search.hcore.spi.BeanResolver hibernateOrmBeanResolver;

	public DelegatingBeanResolver(org.hibernate.search.hcore.spi.BeanResolver hibernateOrmBeanResolver) {
		Objects.requireNonNull( hibernateOrmBeanResolver );
		this.hibernateOrmBeanResolver = hibernateOrmBeanResolver;
	}

	@Override
	public <T> T resolve(Class<?> reference, Class<T> expectedClass) {
		return hibernateOrmBeanResolver.resolve( reference, expectedClass );
	}

}

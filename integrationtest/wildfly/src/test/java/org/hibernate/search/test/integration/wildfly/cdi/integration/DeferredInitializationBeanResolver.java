/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.cdi.integration;

import org.hibernate.search.hcore.spi.BeanResolver;

class DeferredInitializationBeanResolver implements BeanResolver {

	private BeanResolver delegate;

	public void initialize(BeanResolver delegate) {
		this.delegate = delegate;
	}

	@Override
	public <T> T resolve(Class<?> reference, Class<T> expectedClass) {
		return delegate.resolve( reference, expectedClass );
	}
}

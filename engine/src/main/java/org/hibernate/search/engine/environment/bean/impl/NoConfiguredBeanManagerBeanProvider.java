/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class NoConfiguredBeanManagerBeanProvider implements BeanProvider {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	NoConfiguredBeanManagerBeanProvider() {
	}

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public <T> BeanHolder<T> forType(Class<T> typeReference) {
		throw log.noConfiguredBeanManager();
	}

	@Override
	public <T> BeanHolder<T> forTypeAndName(Class<T> typeReference, String implementationFullyQualifiedClassName) {
		throw log.noConfiguredBeanManager();
	}

}

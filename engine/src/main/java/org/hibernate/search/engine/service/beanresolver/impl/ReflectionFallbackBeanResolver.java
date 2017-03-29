/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.service.beanresolver.impl;

import org.hibernate.search.engine.service.beanresolver.spi.BeanResolver;

/**
 * A bean resolver implementing fallback: it attempts to resolve beans
 * against a given delegate, return it if the resolution succeeded (non-null result),
 * or attempts a resolution {@link ReflectionBeanResolver using reflection} if it failed
 * (null result).
 * <p>
 * Any exception thrown by the delegate will be propagated immediately (it won't be swallowed
 * to try reflection).
 * <p>
 * Note that we don't expect this resolver to ever return null, because the resolution
 * using reflection will always either succeed (return a non-null bean) or throw an exception.
 *
 * @author Yoann Rodiere
 */
public class ReflectionFallbackBeanResolver implements BeanResolver {

	private final BeanResolver delegate;
	private final ReflectionBeanResolver fallback;

	public ReflectionFallbackBeanResolver(BeanResolver delegate, ReflectionBeanResolver fallback) {
		super();
		this.delegate = delegate;
		this.fallback = fallback;
	}

	@Override
	public <T> T resolve(Class<?> reference, Class<T> expectedClass) {
		T result = delegate.resolve( reference, expectedClass );
		if ( result == null ) {
			result = fallback.resolve( reference, expectedClass );
		}
		return result;
	}

}

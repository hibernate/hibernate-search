/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.classpath.spi;

import org.hibernate.search.engine.logging.impl.EngineMiscLog;


/**
 * Default implementation of {@code ClassResolver} relying on an {@link AggregatedClassLoader}.
 *
 * @author Hardy Ferentschik
 */
public final class DefaultClassResolver implements ClassResolver {

	private final AggregatedClassLoader aggregatedClassLoader;

	public static ClassResolver create(AggregatedClassLoader aggregatedClassLoader) {
		return new DefaultClassResolver( aggregatedClassLoader );
	}

	private DefaultClassResolver(AggregatedClassLoader aggregatedClassLoader) {
		this.aggregatedClassLoader = aggregatedClassLoader;
	}

	@Override
	public Class<?> classForName(String className) {
		try {
			return Class.forName( className, true, aggregatedClassLoader );
		}
		catch (Exception | LinkageError e) {
			throw EngineMiscLog.INSTANCE.unableToLoadTheClass( className, e.getMessage(), e );
		}
	}

}

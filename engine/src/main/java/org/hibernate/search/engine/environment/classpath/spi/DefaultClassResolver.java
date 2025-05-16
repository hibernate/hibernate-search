/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.classpath.spi;


import org.hibernate.search.engine.logging.impl.EngineMiscLog;
import org.hibernate.search.util.common.annotation.Incubating;

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

	@Override
	public Package packageForName(String packageName) {
		try {
			return Class.forName( packageName + ".package-info", true, aggregatedClassLoader )
					.getPackage();
		}
		catch (Exception | LinkageError e) {
			return null;
		}
	}

	@Incubating
	public AggregatedClassLoader aggregatedClassLoader() {
		return aggregatedClassLoader;
	}
}

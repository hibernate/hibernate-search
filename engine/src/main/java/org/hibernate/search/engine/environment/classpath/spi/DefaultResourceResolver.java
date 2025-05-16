/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.classpath.spi;

import java.io.InputStream;
import java.net.URL;

import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;

/**
 * Default implementation of {@code ClassResolver} relying on an {@link AggregatedClassLoader}.
 *
 * @author Hardy Ferentschik
 */
public final class DefaultResourceResolver implements ResourceResolver {

	private final AggregatedClassLoader aggregatedClassLoader;

	public static ResourceResolver create(AggregatedClassLoader aggregatedClassLoader) {
		return new DefaultResourceResolver( aggregatedClassLoader );
	}

	private DefaultResourceResolver(AggregatedClassLoader aggregatedClassLoader) {
		this.aggregatedClassLoader = aggregatedClassLoader;
	}

	@Override
	@SuppressForbiddenApis(reason = "URL constructors are deprecated in JDK 20+ in favor of using URI.toURL(),"
			+ " but we want to preserve backward compatibility for now (see below).")
	public InputStream locateResourceStream(String name) {
		try {
			final InputStream stream = aggregatedClassLoader.getResourceAsStream( name );
			if ( stream != null ) {
				return stream;
			}
		}
		catch (Exception ignore) {
			// Ignore
		}

		final String stripped = name.startsWith( "/" ) ? name.substring( 1 ) : null;

		if ( stripped != null ) {
			try {
				@SuppressWarnings("deprecation")
				// TODO: HSEARCH-4765 address the URL -> URI constructor change once the URLClassLoader stops using the URL constructor
				//   Main problem: would URI.create(stripped).toURL() fail, since `stripped` seems to be a relative URL?
				//   Do we even have test coverage for this line?
				InputStream resourceStream = new URL( stripped ).openStream();
				return resourceStream;
			}
			catch (Exception ignore) {
				// Ignore
			}

			try {
				final InputStream stream = aggregatedClassLoader.getResourceAsStream( stripped );
				if ( stream != null ) {
					return stream;
				}
			}
			catch (Exception ignore) {
				// Ignore
			}
		}

		return null;
	}

	@Override
	public URL locateResource(String resourceName) {
		try {
			return aggregatedClassLoader.getResource( resourceName );
		}
		catch (Exception e) {
			return null;
		}
	}

}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.classpath.spi;

import java.io.InputStream;
import java.net.URL;

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

}

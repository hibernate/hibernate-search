/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import java.util.Optional;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;

/**
 * Allows integrators to provide their default configuration properties for various scopes that would override
 * Hibernate Search specific ones.
 */
public interface ConfigurationProvider extends Comparable<ConfigurationProvider> {

	/**
	 * @param scope The scope for which configuration properties are about to be used.
	 * @return An empty optional if the provider does not need to override any Hibernate Search specific defaults
	 * for a provided {@code scope}, or a {@link ConfigurationPropertySource} with overrides otherwise.
	 */
	Optional<ConfigurationPropertySource> get(ConfigurationScope scope);

	@Override
	default int compareTo(ConfigurationProvider o) {
		return o != null ? this.getClass().getSimpleName().compareTo( o.getClass().getSimpleName() ) : 1;
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import java.util.Optional;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;

/**
 * Allows integrators to provide their default configuration properties for various scopes that would override
 * Hibernate Search specific ones.
 */
public interface ConfigurationProvider extends Comparable<ConfigurationProvider> {

	/**
	 * Property sources created by this provider <strong>must</strong> follow these rules:
	 * <ul>
	 *     <li>
	 *         A property source only contains properties that are relevant for the scope.
	 *         E.g. a global scope property source should not contain any configuration properties for a specific index.
	 *     </li>
	 *     <li>
	 *         A specific property should only be present in one property source.
	 *         Since these property sources are added as fallbacks a value from the first property source it occurs in will be used
	 *         (starting from the least specific one (global)).
	 *         E.g. if a property is defined in a global scope source and then overridden in a backend/index scope,
	 *         the value will be taken from the global source and backend/index ones will be silently ignored.
	 *     </li>
	 *     <li>
	 *         Property keys must be relative to the scope.
	 *         No keys should start with {@code hibernate.search.} prefix.
	 *         Assuming we want to redefine a property {@code hibernate.search.backend.directory.type} in the backend scope,
	 *         then Hibernate Search would expect that the property source returned by this provider for a backend scope
	 *         will contain a value for a {@code directory.type} key.
	 *     </li>
	 *     <li>
	 *         For global-scope property sources,
	 *         the {@link EngineSpiSettings#BEAN_CONFIGURERS bean_configurers} configuration property
	 *         is only ever looked up if this provider was registered by
	 *         a {@link BeanConfigurer} added through the Java {@link java.util.ServiceLoader} system.
	 *         A provider registered by a {@link BeanConfigurer} added through
	 *         the {@link EngineSpiSettings#BEAN_CONFIGURERS bean_configurers} configuration property
	 *         cannot itself set the {@link EngineSpiSettings#BEAN_CONFIGURERS bean_configurers} configuration property.
	 *     </li>
	 * </ul>
	 *
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

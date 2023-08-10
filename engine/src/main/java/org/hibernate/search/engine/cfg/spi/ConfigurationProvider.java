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
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Allows integrators to provide their default configuration properties for various scopes that would override
 * Hibernate Search specific ones.
 */
@Incubating
public interface ConfigurationProvider {

	/**
	 * Provide a configuration property source for the given scope.
	 * <p>
	 * Property sources created by this provider <strong>must</strong> follow these rules:
	 * <ul>
	 *     <li>
	 *         A property source only contains properties that are relevant for the given scope.
	 *         E.g. a global scope property source should not contain any configuration properties
	 *         for a specific index with the {@code backend.indexes.myindex} prefix;
	 *         if such a property is defined in a global scope source and then overridden in a backend/index scope
	 *         (without the prefix),
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

	/**
	 * Defines a priority of a particular configuration provider.
	 * <p>
	 * If multiple configuration providers are available they will be sorted by their {@link #priority() priority}
	 * and then by FQCN to guarantee a predictable order.
	 *
	 * @return The priority of the current provider.
	 */
	default int priority() {
		return 0;
	}
}

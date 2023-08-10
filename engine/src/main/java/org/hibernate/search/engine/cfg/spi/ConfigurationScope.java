/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import java.util.Objects;
import java.util.function.Predicate;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 Holds a configuration scope.
 * <p>
 * It is <strong>not expected</strong> for service implementors to create the scopes.
 * Hibernate Search will create the scope itself, and it will pass a corresponding scope to the {@link ConfigurationProvider}
 * whenever it is required.
 * <p>
 * Scope always starts as the global scope.
 * This global scope should always be reachable through recursively accessing the {@link #parent() parent scope},
 * unless the current scope is already a global scope.
 * <p>
 * An example of a scope sequence:
 * <ul>
 *     <li>{@code [namespace:global]} -- scope always starts with the global scope</li>
 *     <li>
 *         {@code [namespace:backend name:backend-name]} -- next a backend scope with an <strong>optional</strong> backend named.
 *     		If the name is not present, it means that scope represents a default backend.
 *     	</li>
 *     <li>{@code [namespace:index name:index-name]} -- next is a scope specific to an index</li>
 * </ul>
 * @see ConfigurationScopeNamespaces
 * @see ConfigurationProvider
 */
@Incubating
public final class ConfigurationScope {
	public static final ConfigurationScope GLOBAL = new ConfigurationScope( null, ConfigurationScopeNamespaces.GLOBAL, null );

	private final ConfigurationScope parent;
	private final String namespace;
	private final String name;

	private ConfigurationScope(ConfigurationScope parent, String namespace, String name) {
		this.parent = parent;
		this.namespace = namespace;
		this.name = name;
	}

	public ConfigurationScope reduce(String namespace, String name) {
		return new ConfigurationScope( this, namespace, name );
	}

	public boolean matchAny(String namespace) {
		return this.namespace.equals( namespace );
	}

	public boolean matchExact(String namespace) {
		return matchExact( namespace, null );
	}

	public boolean matchExact(String namespace, String name) {
		return matchAny( namespace ) && Objects.equals( this.name, name );
	}

	public boolean match(Predicate<ConfigurationScope> predicate) {
		return predicate.test( this );
	}

	public ConfigurationScope parent() {
		return parent;
	}

	public String namespace() {
		return namespace;
	}

	public String name() {
		return name;
	}

	@Override
	public String toString() {
		return ( parent == null ? "" : parent + ": " ) + namespace + ( name == null ? "" : "(" + name + ")" );
	}

}

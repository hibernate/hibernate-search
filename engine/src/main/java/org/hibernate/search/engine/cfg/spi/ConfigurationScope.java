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
 * Holds a configuration scope. It is <strong>not expected</strong> for anyone to create the scopes.
 * Hibernate Search will create the scope itself, and it will pass a corresponding scope to the {@link ConfigurationProvider}
 * whenever it is required.
 * <p>
 * Key points about the {@link #reduce(String, String) scope reduction} are:
 * <ul>
 *     <li>Scope always starts as the global scope</li>
 *     <li>If a scope is reduced by some namespace with a name then this scope <strong>must already be reduced</strong> by this namespace <strong>without</strong> the name</li>
 * </ul>
 * For example a sequence of scope reduction can look as:
 * <ul>
 *     <li>{@code [namespace:global]} -- scope always starts with the global scope</li>
 *     <li>{@code [namespace:backend]} -- next is unnamed/default backend scope</li>
 *     <li>{@code [namespace:backend name:backend-name]} -- next there <strong>may</strong> be an <strong>optional</strong> named backend scope</li>
 *     <li>{@code [namespace:index]} -- next is unnamed/default index scope</li>
 *     <li>{@code [namespace:index name:index-name]} -- next is a scope specific to an index with a particular name</li>
 * </ul>
 * @see ConfigurationScopeNamespace
 * @see ConfigurationProvider
 */
@Incubating
public final class ConfigurationScope {
	public static final ConfigurationScope GLOBAL = new ConfigurationScope( null, ConfigurationScopeNamespace.GLOBAL, null );

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

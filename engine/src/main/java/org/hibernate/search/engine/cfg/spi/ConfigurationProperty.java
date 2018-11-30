/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import java.util.Optional;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.KeyContextImpl;

public interface ConfigurationProperty<T> {

	/**
	 * Get the value of this configuration property.
	 *
	 * @param source A configuration source.
	 * @return The value of this property according to the given source.
	 */
	T get(ConfigurationPropertySource source);

	/**
	 * Resolve the key for this configuration property
	 * as registered in the underlying configuration source,
	 * if possible.
	 * <p>
	 * Useful for error messages addressed to the user.
	 *
	 * @param source A configuration source.
	 * @return The value of this property according to the given source.
	 * @see ConfigurationPropertySource#resolve(String)
	 */
	Optional<String> resolve(ConfigurationPropertySource source);

	/**
	 * Resolve the key for this configuration property
	 * as registered in the underlying configuration source,
	 * or, if not possible, just return the "raw" key passed to {@link #forKey(String)}.
	 * <p>
	 * Useful for debugging.
	 *
	 * @param source A configuration source.
	 * @return The value of this property according to the given source.
	 * @see #resolve(ConfigurationPropertySource)
	 */
	String resolveOrRaw(ConfigurationPropertySource source);

	/**
	 * Start the creation of a configuration property.
	 * @param key The key for that configuration property.
	 * @return A context allowing to further define the configuration property.
	 */
	static KeyContext forKey(String key) {
		return new KeyContextImpl( key );
	}

}

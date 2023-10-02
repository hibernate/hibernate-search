/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.spi;

import java.util.Optional;
import java.util.function.Function;

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
	 * Get and transform the value of this configuration property.
	 * <p>
	 * Any exception occurring during transformation will be wrapped in another exception adding some context,
	 * such as the {@link #resolveOrRaw(ConfigurationPropertySource) resolved key} for this property.
	 *
	 * @param source A configuration source.
	 * @param transform A transform function to be applied to the value of this configuration property
	 * before returning the result.
	 * @param <R> The transformed type.
	 * @return The value of this property according to the given source.
	 */
	<R> R getAndTransform(ConfigurationPropertySource source, Function<T, R> transform);

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

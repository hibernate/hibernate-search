/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg;

import java.util.Optional;

import org.hibernate.search.engine.cfg.impl.EmptyConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.FallbackConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.MaskedConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.OverriddenConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.PrefixedConfigurationPropertySource;

/**
 * A source of property values for Hibernate Search.
 * <p>
 * This is effectively a key-value store,
 * with a few features that make it convenient when retrieving configuration property values.
 */
public interface ConfigurationPropertySource {

	/**
	 * @param key The key of the property to get.
	 * @return An optional containing the value of the requested property,
	 * or <code>Optional.empty()</code> if the property is missing.
	 */
	Optional<?> get(String key);

	/**
	 * @param key The key of the property to get.
	 * @return An optional containing the key as registered in the underlying configuration source,
	 * with any transformation ({@link #withPrefix(String) prefixes}, {@link #withMask(String) masks})
	 * reverted.
	 * Whether the optional is empty is not related to the key having a value in the underlying configuration source;
	 * instead, the optional is empty only if the key cannot possibly be registered in the underlying configuration source,
	 * e.g. if the key is missing a mandatory prefix.
	 */
	Optional<String> resolve(String key);

	/**
	 * @param prefix A prefix to prepend to configuration properties.
	 * @return A source containing the same properties as this source, but prefixed with the given prefix plus ".".
	 */
	default ConfigurationPropertySource withPrefix(String prefix) {
		return new PrefixedConfigurationPropertySource( this, prefix );
	}

	/**
	 * @param mask A mask to filter the properties with.
	 * @return A source containing only the properties of this source that start with the given mask plus ".".
	 */
	default ConfigurationPropertySource withMask(String mask) {
		return new MaskedConfigurationPropertySource( this, mask );
	}

	/**
	 * Create a new configuration source which falls back to another source when a property is missing in this source.
	 * <p>
	 * {@code main.withFallback( fallback )} is equivalent to {@code fallback.withOverride( main )}
	 * except for one detail: in the first example, a call to {@link #resolve(String)} on the resulting source
	 * will resolve the key against {@code main}, but in the second example it will resolve the key against {@code override}.
	 *
	 * @param fallback A fallback source.
	 * @return A source containing the same properties as this source, plus any property from <code>fallback</code>
	 * that isn't in this source.
	 */
	default ConfigurationPropertySource withFallback(ConfigurationPropertySource fallback) {
		return new FallbackConfigurationPropertySource( this, fallback );
	}

	/**
	 * Create a new configuration source which overrides the properties defined in this source.
	 * <p>
	 * {@code main.withOverride( override )} is equivalent to {@code override.withFallback( main )}
	 * except for one detail: in the first example, a call to {@link #resolve(String)} on the resulting source
	 * will resolve the key against {@code main}, but in the second example it will resolve the key against {@code override}.
	 *
	 * @param override An overriding source.
	 * @return A source containing the same properties as this source,
	 * overridden by the properties from <code>override</code>,
	 * and augmented by the properties from <code>override</code> that are not in this source.
	 */
	default ConfigurationPropertySource withOverride(ConfigurationPropertySource override) {
		return new OverriddenConfigurationPropertySource( this, override );
	}

	/**
	 * @return A source without any property.
	 */
	static ConfigurationPropertySource empty() {
		return EmptyConfigurationPropertySource.get();
	}
}

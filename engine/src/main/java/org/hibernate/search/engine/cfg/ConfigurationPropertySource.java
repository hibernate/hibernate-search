/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg;

import java.util.Optional;
import java.util.Properties;

import org.hibernate.search.engine.cfg.impl.EmptyConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.FallbackConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.MaskedConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.PrefixedConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.PropertiesConfigurationPropertySource;

public interface ConfigurationPropertySource {

	/**
	 * @param key The key of the property to get.
	 * @return An optional containing the value of the requested property,
	 * or <code>Optional.empty()</code> if the property is missing.
	 */
	Optional<?> get(String key);

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
	 * @param fallback A fallback source.
	 * @return A source containing the same properties as this source, plus any property from <code>fallback</code>
	 * that isn't in this source.
	 */
	default ConfigurationPropertySource withFallback(ConfigurationPropertySource fallback) {
		return new FallbackConfigurationPropertySource( this, fallback );
	}

	/**
	 * @param properties The {@link Properties} object to extract properties from.
	 * @return An source containing the properties from the given {@link Properties} object.
	 */
	static ConfigurationPropertySource fromProperties(Properties properties) {
		return new PropertiesConfigurationPropertySource( properties );
	}

	/**
	 * @return An source without any property.
	 */
	static ConfigurationPropertySource empty() {
		return EmptyConfigurationPropertySource.get();
	}
}

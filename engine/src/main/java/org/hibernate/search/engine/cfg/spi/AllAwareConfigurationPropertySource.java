/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.MapConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.SystemConfigurationPropertySource;

/**
 * A source of property values for Hibernate Search with knowledge of the full set of properties.
 * <p>
 * Implementations provide, on top of the usual key lookup,
 * a way to retrieve <strong>all</strong> keys with a given prefix,
 * which allows checking that all property keys were consumed, in particular.
 */
public interface AllAwareConfigurationPropertySource extends ConfigurationPropertySource {

	Set<String> resolveAll(BiPredicate<String, Object> predicate);

	/**
	 * @param map The {@link Map} object to extract property values from.
	 * @return A source containing the properties from the given {@link Map} object.
	 */
	static AllAwareConfigurationPropertySource fromMap(Map<String, ?> map) {
		return new MapConfigurationPropertySource( map );
	}

	/**
	 * @return A source containing the system properties ({@link System#getProperty(String)}).
	 */
	static AllAwareConfigurationPropertySource system() {
		return SystemConfigurationPropertySource.get();
	}

}

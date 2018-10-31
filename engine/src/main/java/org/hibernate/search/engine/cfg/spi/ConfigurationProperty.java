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

	T get(ConfigurationPropertySource source);

	Optional<String> resolve(ConfigurationPropertySource source);

	String resolveOrRaw(ConfigurationPropertySource source);

	static KeyContext forKey(String key) {
		return new KeyContextImpl( key );
	}

}

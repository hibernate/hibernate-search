/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.PropertyContext;

class PropertyContextImpl<T> implements PropertyContext<T> {
	private final String key;
	private final Function<Optional<?>, T> converter;

	PropertyContextImpl(String key, Function<Optional<?>, T> converter) {
		this.key = key;
		this.converter = converter;
	}

	@Override
	public ConfigurationProperty<T> build() {
		return new FunctionConfigurationProperty<>( key, converter );
	}
}

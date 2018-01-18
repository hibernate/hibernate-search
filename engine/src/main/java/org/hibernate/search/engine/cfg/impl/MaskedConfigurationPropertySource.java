/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.Objects;
import java.util.Optional;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;

public class MaskedConfigurationPropertySource implements ConfigurationPropertySource {
	private final ConfigurationPropertySource toMask;
	private final String radix;

	public MaskedConfigurationPropertySource(ConfigurationPropertySource toMask, String mask) {
		this.toMask = Objects.requireNonNull( toMask );
		this.radix = Objects.requireNonNull( mask ) + ".";
	}

	@Override
	public Optional<?> get(String key) {
		String compositeKey = radix + key;
		return toMask.get( compositeKey );
	}

	@Override
	public ConfigurationPropertySource withMask(String mask) {
		return new MaskedConfigurationPropertySource( toMask, radix + mask );
	}
}

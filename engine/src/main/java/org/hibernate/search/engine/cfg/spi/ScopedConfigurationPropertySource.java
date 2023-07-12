/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.FallbackConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.MaskedConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.OverriddenConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.PrefixedConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.RescopableConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface ScopedConfigurationPropertySource extends ConfigurationPropertySource {

	static ScopedConfigurationPropertySource wrap(BeanResolver resolver,
			ConfigurationPropertySource configurationPropertySource) {
		return new RescopableConfigurationPropertySource( resolver, configurationPropertySource );
	}

	default ScopedConfigurationPropertySource withScope(BeanResolver beanResolver, String namespace) {
		return withScope( beanResolver, namespace, null );
	}

	ScopedConfigurationPropertySource withScope(BeanResolver beanResolver, String namespace, String name);

	@Override
	default ScopedConfigurationPropertySource withPrefix(String prefix) {
		return new PrefixedConfigurationPropertySource( this, prefix );
	}

	@Override
	default ScopedConfigurationPropertySource withMask(String mask) {
		return new MaskedConfigurationPropertySource( this, mask );
	}

	@Override
	default ScopedConfigurationPropertySource withFallback(ConfigurationPropertySource fallback) {
		return new FallbackConfigurationPropertySource( this, fallback );
	}

	@Override
	default ScopedConfigurationPropertySource withOverride(ConfigurationPropertySource override) {
		return new OverriddenConfigurationPropertySource( this, override );
	}
}

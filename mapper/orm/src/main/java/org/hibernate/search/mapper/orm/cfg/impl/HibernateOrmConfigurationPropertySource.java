/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.cfg.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.Set;

import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.UnusedPropertyTrackingConfigurationPropertySource;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

public class HibernateOrmConfigurationPropertySource implements ConfigurationPropertySource {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<Boolean> ENABLE_CONFIGURATION_PROPERTY_TRACKING =
			// We don't use the radical here, but the full property key: the property is retrieved before we apply the mask
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.ENABLE_CONFIGURATION_PROPERTY_TRACKING )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.ENABLE_CONFIGURATION_PROPERTY_TRACKING )
					.build();

	private final UnusedPropertyTrackingConfigurationPropertySource unusedPropertyTrackingPropertySource;
	private final ConfigurationPropertySource delegate;

	public HibernateOrmConfigurationPropertySource(ConfigurationService configurationService) {
		HibernateOrmConfigurationServicePropertySource serviceSource = new HibernateOrmConfigurationServicePropertySource( configurationService );
		ConfigurationPropertySource maskedSource = serviceSource.withMask( "hibernate.search" );

		if ( ENABLE_CONFIGURATION_PROPERTY_TRACKING.get( maskedSource ) ) {
			Set<String> availablePropertyKeys = serviceSource.resolveAll( HibernateOrmMapperSettings.PREFIX );
			unusedPropertyTrackingPropertySource =
					new UnusedPropertyTrackingConfigurationPropertySource( maskedSource, availablePropertyKeys );
			// Make sure to mark the "enable configuration property tracking" property as used
			ENABLE_CONFIGURATION_PROPERTY_TRACKING.get( unusedPropertyTrackingPropertySource );
			delegate = unusedPropertyTrackingPropertySource;
		}
		else {
			log.configurationPropertyTrackingDisabled();
			unusedPropertyTrackingPropertySource = null;
			delegate = maskedSource;
		}
	}

	@Override
	public Optional<?> get(String key) {
		return delegate.get( key );
	}

	@Override
	public Optional<String> resolve(String key) {
		return delegate.resolve( key );
	}

	public void afterBootstrap() {
		if ( unusedPropertyTrackingPropertySource != null ) {
			Set<String> unusedPropertyKeys = unusedPropertyTrackingPropertySource.getUnusedPropertyKeys();
			if ( !unusedPropertyKeys.isEmpty() ) {
				log.configurationPropertyTrackingUnusedProperties(
						unusedPropertyKeys,
						ENABLE_CONFIGURATION_PROPERTY_TRACKING.resolveOrRaw( this )
				);
			}
		}
	}
}

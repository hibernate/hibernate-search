/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.cfg.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.ConsumedPropertyTrackingConfigurationPropertySource;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class HibernateOrmConfigurationPropertySource implements ConfigurationPropertySource {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<Boolean> ENABLE_CONFIGURATION_PROPERTY_TRACKING =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.ENABLE_CONFIGURATION_PROPERTY_TRACKING )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.ENABLE_CONFIGURATION_PROPERTY_TRACKING )
					.build();

	private final HibernateOrmConfigurationServicePropertySource configurationServiceSource;
	private final ConsumedPropertyTrackingConfigurationPropertySource consumedPropertyTrackingPropertySource;
	private final ConfigurationPropertySource delegate;

	public HibernateOrmConfigurationPropertySource(ConfigurationService configurationService) {
		this.configurationServiceSource = new HibernateOrmConfigurationServicePropertySource( configurationService );
		ConfigurationPropertySource maskedSource = configurationServiceSource.withMask( "hibernate.search" );

		if ( ENABLE_CONFIGURATION_PROPERTY_TRACKING.get( maskedSource ) ) {
			consumedPropertyTrackingPropertySource =
					new ConsumedPropertyTrackingConfigurationPropertySource( maskedSource );
			// Make sure to mark the "enable configuration property tracking" property as used
			ENABLE_CONFIGURATION_PROPERTY_TRACKING.get( consumedPropertyTrackingPropertySource );
			delegate = consumedPropertyTrackingPropertySource;
		}
		else {
			consumedPropertyTrackingPropertySource = null;
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

	public Map<?, ?> getAllRawProperties() {
		return configurationServiceSource.getAllRawProperties();
	}

	public Optional<ConsumedPropertyKeysReport> getConsumedPropertiesReport() {
		if ( consumedPropertyTrackingPropertySource == null ) {
			return Optional.empty();
		}
		else {
			return Optional.of( new ConsumedPropertyKeysReport(
					configurationServiceSource.resolveAll( HibernateOrmMapperSettings.PREFIX ),
					consumedPropertyTrackingPropertySource.getConsumedPropertyKeys()
			) );
		}
	}

	public void beforeBoot() {
		if ( consumedPropertyTrackingPropertySource == null ) {
			log.configurationPropertyTrackingDisabled();
		}
	}

	public void afterBoot(Optional<ConsumedPropertyKeysReport> previousReport) {
		List<Optional<ConsumedPropertyKeysReport>> reports =
				CollectionHelper.asImmutableList( previousReport, getConsumedPropertiesReport() );
		Set<String> unconsumedPropertyKeys = new LinkedHashSet<>();

		// Add all available property keys
		for ( Optional<ConsumedPropertyKeysReport> report : reports ) {
			if ( report.isPresent() ) {
				unconsumedPropertyKeys.addAll( report.get().getAvailablePropertyKeys() );
			}
		}

		// Remove all consumed property keys
		for ( Optional<ConsumedPropertyKeysReport> report : reports ) {
			if ( report.isPresent() ) {
				unconsumedPropertyKeys.removeAll( report.get().getConsumedPropertyKeys() );
			}
		}

		if ( !unconsumedPropertyKeys.isEmpty() ) {
			log.configurationPropertyTrackingUnusedProperties(
					unconsumedPropertyKeys,
					ENABLE_CONFIGURATION_PROPERTY_TRACKING.resolveOrRaw( this )
			);
		}
	}

}

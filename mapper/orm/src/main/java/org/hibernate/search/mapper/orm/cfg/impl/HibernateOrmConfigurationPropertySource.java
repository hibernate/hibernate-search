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
import org.hibernate.search.mapper.orm.cfg.HibernateOrmConfigurationPropertyCheckingStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class HibernateOrmConfigurationPropertySource implements ConfigurationPropertySource {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<HibernateOrmConfigurationPropertyCheckingStrategyName>
			CONFIGURATION_PROPERTY_CHECKING_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.CONFIGURATION_PROPERTY_CHECKING_STRATEGY )
					.as( HibernateOrmConfigurationPropertyCheckingStrategyName.class, HibernateOrmConfigurationPropertyCheckingStrategyName::of )
					.withDefault( HibernateOrmMapperSettings.Defaults.CONFIGURATION_PROPERTY_CHECKING_STRATEGY )
					.build();

	private final HibernateOrmConfigurationServicePropertySource configurationServiceSource;
	private final ConsumedPropertyTrackingConfigurationPropertySource consumedPropertyTrackingPropertySource;
	private final ConfigurationPropertySource delegate;

	public HibernateOrmConfigurationPropertySource(ConfigurationService configurationService) {
		this.configurationServiceSource = new HibernateOrmConfigurationServicePropertySource( configurationService );
		ConfigurationPropertySource maskedSource = configurationServiceSource.withMask( "hibernate.search" );

		HibernateOrmConfigurationPropertyCheckingStrategyName checkingStrategy =
				CONFIGURATION_PROPERTY_CHECKING_STRATEGY.get( maskedSource );
		switch ( checkingStrategy ) {
			case WARN:
				consumedPropertyTrackingPropertySource =
						new ConsumedPropertyTrackingConfigurationPropertySource( maskedSource );
				// Make sure to mark the "enable configuration property tracking" property as used
				CONFIGURATION_PROPERTY_CHECKING_STRATEGY.get( consumedPropertyTrackingPropertySource );
				delegate = consumedPropertyTrackingPropertySource;
				break;
			case IGNORE:
				consumedPropertyTrackingPropertySource = null;
				delegate = maskedSource;
				break;
			default:
				throw new AssertionFailure(
						"Unexpected configuration property checking strategy name: " + checkingStrategy
				);
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
					CONFIGURATION_PROPERTY_CHECKING_STRATEGY.resolveOrRaw( this ),
					HibernateOrmConfigurationPropertyCheckingStrategyName.IGNORE.getExternalRepresentation()
			);
		}
	}

}

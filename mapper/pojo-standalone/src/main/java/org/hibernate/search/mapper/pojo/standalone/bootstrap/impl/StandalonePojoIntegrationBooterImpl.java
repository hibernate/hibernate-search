/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.bootstrap.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertyChecker;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.common.spi.SearchIntegrationEnvironment;
import org.hibernate.search.engine.common.spi.SearchIntegrationPartialBuildState;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.mapper.pojo.standalone.bootstrap.spi.StandalonePojoIntegrationBooter;
import org.hibernate.search.mapper.pojo.standalone.bootstrap.spi.StandalonePojoIntegrationBooterBehavior;
import org.hibernate.search.mapper.pojo.standalone.cfg.spi.StandalonePojoMapperSpiSettings;
import org.hibernate.search.mapper.pojo.standalone.logging.impl.Log;
import org.hibernate.search.mapper.pojo.standalone.mapping.impl.StandalonePojoMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.impl.StandalonePojoMappingInitiator;
import org.hibernate.search.mapper.pojo.standalone.mapping.impl.StandalonePojoMappingKey;
import org.hibernate.search.mapper.pojo.standalone.model.impl.StandalonePojoBootstrapIntrospector;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;

public class StandalonePojoIntegrationBooterImpl implements StandalonePojoIntegrationBooter {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final OptionalConfigurationProperty<BeanProvider> BEAN_PROVIDER =
			ConfigurationProperty.forKey( StandalonePojoMapperSpiSettings.BEAN_PROVIDER )
					.as( BeanProvider.class, value -> {
						throw log.invalidStringForBeanProvider( value, BeanProvider.class );
					} )
					.build();

	private final ConfigurationPropertyChecker propertyChecker;
	private final ValueHandleFactory valueHandleFactory;
	private final ConfigurationPropertySource propertySource;

	private StandalonePojoIntegrationBooterImpl(BuilderImpl builder) {
		propertyChecker = ConfigurationPropertyChecker.create();
		valueHandleFactory = builder.valueHandleFactory;

		propertySource = propertyChecker.wrap(
				AllAwareConfigurationPropertySource.fromMap( builder.properties )
		);
	}

	@Override
	public void preBoot(BiConsumer<String, Object> propertyCollector) {
		doBootFirstPhase()
				.set( propertyCollector );
	}

	private StandalonePojoIntegrationPartialBuildState getPartialBuildStateOrDoBootFirstPhase() {
		Optional<StandalonePojoIntegrationPartialBuildState> partialBuildState =
				StandalonePojoIntegrationPartialBuildState.get( propertySource );
		if ( partialBuildState.isPresent() ) {
			return partialBuildState.get();
		}
		else {
			// Most common path (except for Quarkus): Hibernate Search wasn't pre-booted ahead of time,
			// so we will need to perform the first phase of boot now.
			//
			// Do not remove the use of StandalonePojoIntegrationBooterBehavior as an intermediary:
			// its implementation is overridden by Quarkus to make it clear to SubstrateVM
			// that the first phase of boot is never executed in the native binary.
			return StandalonePojoIntegrationBooterBehavior.bootFirstPhase( this::doBootFirstPhase );
		}
	}

	private StandalonePojoIntegrationPartialBuildState doBootFirstPhase() {
		StandalonePojoBootstrapIntrospector introspector =
				StandalonePojoBootstrapIntrospector.create( valueHandleFactory != null
						? valueHandleFactory
						: ValueHandleFactory.usingMethodHandle( MethodHandles.publicLookup() ) );
		StandalonePojoMappingKey mappingKey = new StandalonePojoMappingKey();
		StandalonePojoMappingInitiator mappingInitiator = new StandalonePojoMappingInitiator( introspector );

		SearchIntegrationEnvironment environment = null;
		SearchIntegrationPartialBuildState integrationPartialBuildState = null;
		try {
			environment = createEnvironment();

			SearchIntegration.Builder integrationBuilder = SearchIntegration.builder( environment );
			integrationBuilder.addMappingInitiator( mappingKey, mappingInitiator );

			integrationPartialBuildState = integrationBuilder.prepareBuild();

			return new StandalonePojoIntegrationPartialBuildState( integrationPartialBuildState, mappingKey );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( environment )
					.push( SearchIntegrationPartialBuildState::closeOnFailure, integrationPartialBuildState );
			throw e;
		}
	}

	private SearchIntegrationEnvironment createEnvironment() {
		SearchIntegrationEnvironment.Builder environmentBuilder =
				SearchIntegrationEnvironment.builder( propertySource, propertyChecker );
		BEAN_PROVIDER.get( propertySource ).ifPresent( environmentBuilder::beanProvider );
		return environmentBuilder.build();
	}

	@Override
	public StandalonePojoMapping boot() {
		StandalonePojoIntegrationPartialBuildState partialBuildState = getPartialBuildStateOrDoBootFirstPhase();

		try {
			return partialBuildState.doBootSecondPhase( propertySource, propertyChecker );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( StandalonePojoIntegrationPartialBuildState::closeOnFailure, partialBuildState );
			throw e;
		}
	}

	public static class BuilderImpl implements Builder {
		private ValueHandleFactory valueHandleFactory;
		private final Map<String, Object> properties = new HashMap<>();

		public BuilderImpl() {
		}

		@Override
		public BuilderImpl valueReadHandleFactory(ValueHandleFactory valueHandleFactory) {
			this.valueHandleFactory = valueHandleFactory;
			return this;
		}

		public BuilderImpl property(String name, Object value) {
			properties.put( name, value );
			return this;
		}

		public BuilderImpl properties(Map<String, ?> map) {
			properties.putAll( map );
			return this;
		}

		@Override
		public StandalonePojoIntegrationBooterImpl build() {
			return new StandalonePojoIntegrationBooterImpl( this );
		}
	}
}

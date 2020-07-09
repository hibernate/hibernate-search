/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.performance.backend.base.testsupport.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertyChecker;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.common.spi.SearchIntegrationBuilder;
import org.hibernate.search.engine.common.spi.SearchIntegrationFinalizer;
import org.hibernate.search.engine.common.spi.SearchIntegrationPartialBuildState;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.filesystem.TemporaryFileHolder;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingInitiator;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingKey;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@State(Scope.Thread)
public abstract class AbstractBackendHolder {

	public static final int INDEX_COUNT = 3;

	private static final String BACKEND_NAME = "testedBackend";

	private SearchIntegration integration;
	private List<MappedIndex> indexes;

	@Setup(Level.Trial)
	@SuppressWarnings("deprecation")
	public void startHibernateSearch(TemporaryFileHolder temporaryFileHolder) throws IOException {
		Map<String, Object> baseProperties = new LinkedHashMap<>();
		baseProperties.put( EngineSettings.DEFAULT_BACKEND, BACKEND_NAME );

		ConfigurationPropertySource configurationFromParameter =
				ConfigurationPropertySource.fromMap( stringToMap( getConfigurationParameter() ) );

		ConfigurationPropertySource propertySource = ConfigurationPropertySource.fromMap( baseProperties )
				.withOverride(
						getDefaultBackendProperties( temporaryFileHolder )
								// Allow overrides at the backend level using system properties
								.withOverride( ConfigurationPropertySource.system() )
								// Allow multiple backend configurations to be tested using a benchmark parameter
								// > Apply the configuration at the backend level
								.withOverride( configurationFromParameter )
								// > Apply the configuration at the index level (for convenience)
								.withOverride( configurationFromParameter.withPrefix( BackendSettings.INDEX_DEFAULTS ) )
								.withPrefix( EngineSettings.BACKENDS + "." + BACKEND_NAME )
				);

		ConfigurationPropertyChecker unusedPropertyChecker = ConfigurationPropertyChecker.create();

		SearchIntegrationBuilder integrationBuilder =
				SearchIntegration.builder( propertySource, unusedPropertyChecker );

		StubMappingInitiator initiator = new StubMappingInitiator( false );
		StubMappingKey mappingKey = new StubMappingKey();
		integrationBuilder.addMappingInitiator( mappingKey, initiator );

		indexes = new ArrayList<>();
		for ( int i = 0; i < INDEX_COUNT; ++i ) {
			MappedIndex index = new MappedIndex( BACKEND_NAME, i );
			initiator.add( index );
			indexes.add( index );
		}

		SearchIntegrationPartialBuildState integrationPartialBuildState = integrationBuilder.prepareBuild();
		try {
			SearchIntegrationFinalizer finalizer =
					integrationPartialBuildState.finalizer( propertySource, unusedPropertyChecker );
			finalizer.finalizeMapping(
					mappingKey,
					(context, partialMapping) ->
							partialMapping.finalizeMapping( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_AND_DROP )
			);
			integration = finalizer.finalizeIntegration();
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( SearchIntegrationPartialBuildState::closeOnFailure, integrationPartialBuildState );
			throw e;
		}
	}

	@Setup(Level.Iteration)
	public void initializeIndexes(IndexInitializer indexInitializer) {
		indexInitializer.intializeIndexes( indexes );
	}

	@TearDown(Level.Trial)
	public void stopHibernateSearch() {
		if ( integration != null ) {
			integration.close();
		}
	}

	public List<MappedIndex> getIndexes() {
		return indexes;
	}

	protected final Map<String, String> stringToMap(String settings) {
		String[] settingsSplit = settings.split( "&" );
		Map<String, String> map = new LinkedHashMap<>();
		for ( String keyValue : settingsSplit ) {
			if ( keyValue.isEmpty() ) {
				continue;
			}
			String[] keyValueSplit = keyValue.split( "=" );
			map.put( keyValueSplit[0], keyValueSplit[1] );
		}
		return map;
	}

	protected abstract ConfigurationPropertySource getDefaultBackendProperties(TemporaryFileHolder temporaryFileHolder)
			throws IOException;

	protected abstract String getConfigurationParameter();
}

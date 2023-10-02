/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.performance.backend.base.testsupport.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertyChecker;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.common.spi.SearchIntegrationEnvironment;
import org.hibernate.search.engine.common.spi.SearchIntegrationFinalizer;
import org.hibernate.search.engine.common.spi.SearchIntegrationPartialBuildState;
import org.hibernate.search.engine.tenancy.spi.TenancyMode;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.filesystem.TemporaryFileHolder;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingBackendFeatures;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingImpl;
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

	private StubMappingImpl mapping;
	private List<MappedIndex> indexes;

	@Setup(Level.Trial)
	@SuppressWarnings("resource") // For the eclipse-compiler: complains on StubMappingImpl not bing closed
	public void startHibernateSearch(TemporaryFileHolder temporaryFileHolder) throws IOException {
		Map<String, Object> baseProperties = new LinkedHashMap<>();

		ConfigurationPropertySource configurationFromParameter =
				AllAwareConfigurationPropertySource.fromMap( stringToMap( getConfigurationParameter() ) );

		ConfigurationPropertySource propertySource = AllAwareConfigurationPropertySource.fromMap( baseProperties )
				.withOverride(
						getDefaultBackendProperties( temporaryFileHolder )
								// Allow overrides at the backend level using system properties.
								.withOverride( AllAwareConfigurationPropertySource.system() )
								// Allow multiple backend configurations to be tested using a benchmark parameter.
								// This configuration can set backend properties or set defaults for index properties.
								.withOverride( configurationFromParameter )
								.withPrefix( EngineSettings.BACKEND )
				);

		ConfigurationPropertyChecker unusedPropertyChecker = ConfigurationPropertyChecker.create();

		SearchIntegrationEnvironment environment =
				SearchIntegrationEnvironment.builder( propertySource, unusedPropertyChecker ).build();

		SearchIntegration.Builder integrationBuilder =
				SearchIntegration.builder( environment );

		StubMappingInitiator initiator = new StubMappingInitiator( new StubMappingBackendFeatures() {},
				TenancyMode.SINGLE_TENANCY );
		StubMappingKey mappingKey = new StubMappingKey();
		integrationBuilder.addMappingInitiator( mappingKey, initiator );

		indexes = new ArrayList<>();
		for ( int i = 0; i < INDEX_COUNT; ++i ) {
			MappedIndex index = new MappedIndex( i );
			initiator.add( index );
			indexes.add( index );
		}

		SearchIntegrationPartialBuildState integrationPartialBuildState = integrationBuilder.prepareBuild();
		try {
			SearchIntegrationFinalizer finalizer =
					integrationPartialBuildState.finalizer( propertySource, unusedPropertyChecker );
			mapping = finalizer.finalizeMapping(
					mappingKey,
					(context, partialMapping) -> partialMapping.finalizeMapping(
							StubMappingSchemaManagementStrategy.DROP_AND_CREATE_AND_DROP )
			);
			finalizer.finalizeIntegration();
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
		if ( mapping != null ) {
			mapping.close();
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

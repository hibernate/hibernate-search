/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientUtils;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.dialect.impl.ElasticsearchDialectFactory;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.ElasticsearchProtocolDialect;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.link.impl.ElasticsearchLink;
import org.hibernate.search.backend.elasticsearch.logging.impl.MappingLog;
import org.hibernate.search.backend.elasticsearch.logging.impl.VersionLog;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.metadata.impl.ElasticsearchIndexMetadataSyntax;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.backend.elasticsearch.mapping.impl.TypeNameMapping;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.resources.impl.BackendThreads;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionExtractionHelper;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionBackendContext;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchResultExtractorFactory;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.ElasticsearchWorkFactory;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;

import com.google.gson.GsonBuilder;

class ElasticsearchLinkImpl implements ElasticsearchLink {

	static final OptionalConfigurationProperty<ElasticsearchVersion> VERSION =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.VERSION )
					.as( ElasticsearchVersion.class, ElasticsearchVersion::of )
					.build();

	private static final OptionalConfigurationProperty<Boolean> VERSION_CHECK_ENABLED =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.VERSION_CHECK_ENABLED )
					.asBoolean()
					.build();

	private static final ConfigurationProperty<Integer> SCROLL_TIMEOUT =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.SCROLL_TIMEOUT )
					.asIntegerStrictlyPositive()
					.withDefault( ElasticsearchBackendSettings.Defaults.SCROLL_TIMEOUT )
					.build();

	private static final ConfigurationProperty<Boolean> QUERY_SHARD_FAILURE_IGNORE =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.QUERY_SHARD_FAILURE_IGNORE )
					.asBoolean()
					.withDefault( ElasticsearchBackendSettings.Defaults.QUERY_SHARD_FAILURE_IGNORE )
					.build();

	private static final ConfigurationProperty<BeanReference<? extends IndexLayoutStrategy>> LAYOUT_STRATEGY =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.LAYOUT_STRATEGY )
					.asBeanReference( IndexLayoutStrategy.class )
					.withDefault( ElasticsearchBackendSettings.Defaults.LAYOUT_STRATEGY )
					.build();

	private final BeanHolder<? extends ElasticsearchClientFactory> clientFactoryHolder;
	private final BackendThreads threads;
	private final GsonProvider defaultGsonProvider;
	private final boolean logPrettyPrinting;
	private final ElasticsearchDialectFactory dialectFactory;
	private final Optional<ElasticsearchVersion> configuredVersionOnBackendCreationOptional;
	private final TypeNameMapping typeNameMapping;
	private final IndexNamesRegistry indexNamesRegistry;

	private ElasticsearchClientImplementor clientImplementor;
	private ElasticsearchVersion elasticsearchVersion;
	private GsonProvider gsonProvider;
	private ElasticsearchIndexMetadataSyntax indexMetadataSyntax;
	private ElasticsearchSearchSyntax searchSyntax;
	private ElasticsearchWorkFactory workFactory;
	private ElasticsearchSearchResultExtractorFactory searchResultExtractorFactory;
	private Integer scrollTimeout;
	private BeanHolder<? extends IndexLayoutStrategy> indexLayoutStrategyHolder;
	private SearchProjectionBackendContext searchProjectionBackendContext;

	ElasticsearchLinkImpl(BeanHolder<? extends ElasticsearchClientFactory> clientFactoryHolder,
			BackendThreads threads, GsonProvider defaultGsonProvider, boolean logPrettyPrinting,
			ElasticsearchDialectFactory dialectFactory,
			Optional<ElasticsearchVersion> configuredVersionOnBackendCreationOptional,
			TypeNameMapping typeNameMapping) {
		this.clientFactoryHolder = clientFactoryHolder;
		this.threads = threads;
		this.defaultGsonProvider = defaultGsonProvider;
		this.logPrettyPrinting = logPrettyPrinting;
		this.dialectFactory = dialectFactory;
		this.configuredVersionOnBackendCreationOptional = configuredVersionOnBackendCreationOptional;
		this.typeNameMapping = typeNameMapping;
		this.indexNamesRegistry = new IndexNamesRegistry();
	}

	@Override
	public ElasticsearchClient getClient() {
		checkStarted();
		return clientImplementor;
	}

	@Override
	public GsonProvider getGsonProvider() {
		checkStarted();
		return gsonProvider;
	}

	@Override
	public ElasticsearchIndexMetadataSyntax getIndexMetadataSyntax() {
		checkStarted();
		return indexMetadataSyntax;
	}

	@Override
	public ElasticsearchSearchSyntax getSearchSyntax() {
		checkStarted();
		return searchSyntax;
	}

	@Override
	public ElasticsearchWorkFactory getWorkFactory() {
		checkStarted();
		return workFactory;
	}

	@Override
	public ElasticsearchSearchResultExtractorFactory getSearchResultExtractorFactory() {
		checkStarted();
		return searchResultExtractorFactory;
	}

	@Override
	public Integer getScrollTimeout() {
		checkStarted();
		return scrollTimeout;
	}

	@Override
	public IndexLayoutStrategy getIndexLayoutStrategy() {
		checkStarted();
		return indexLayoutStrategyHolder.get();
	}

	@Override
	public TypeNameMapping getTypeNameMapping() {
		return typeNameMapping;
	}

	@Override
	public IndexNames createIndexNames(String hibernateSearchIndexName, String mappedTypeName) {
		checkStarted();

		IndexLayoutStrategy indexLayoutStrategy = indexLayoutStrategyHolder.get();
		URLEncodedString writeAlias = IndexNames.encodeName( indexLayoutStrategy.createWriteAlias( hibernateSearchIndexName ) );
		URLEncodedString readAlias = IndexNames.encodeName( indexLayoutStrategy.createReadAlias( hibernateSearchIndexName ) );

		URLEncodedString primaryName = null;
		if ( writeAlias == null || readAlias == null ) {
			primaryName = IndexNames.encodeName(
					indexLayoutStrategy.createInitialElasticsearchIndexName( hibernateSearchIndexName ) );
		}
		else if ( writeAlias.equals( readAlias ) ) {
			throw MappingLog.INSTANCE.sameWriteAndReadAliases( writeAlias );
		}

		URLEncodedString readName = readAlias != null ? readAlias : primaryName;
		URLEncodedString writeName = writeAlias != null ? writeAlias : primaryName;

		IndexNames indexNames = new IndexNames( hibernateSearchIndexName,
				writeName, writeAlias != null, readName, readAlias != null );

		// This will check that names are unique.
		indexNamesRegistry.register( indexNames );

		// This will allow the type mapping to resolve the type name from the index name.
		typeNameMapping.register( indexNames, mappedTypeName );

		return indexNames;
	}

	@Override
	public SearchProjectionBackendContext getSearchProjectionBackendContext() {
		checkStarted();
		return searchProjectionBackendContext;
	}

	ElasticsearchVersion getElasticsearchVersion() {
		checkStarted();
		return elasticsearchVersion;
	}

	void onStart(BeanResolver beanResolver, MultiTenancyStrategy multiTenancyStrategy,
			ConfigurationPropertySource propertySource) {
		if ( clientImplementor == null ) {
			clientImplementor = clientFactoryHolder.get().create(
					beanResolver, propertySource, threads.getThreadProvider(), threads.getPrefix(),
					threads.getWorkExecutor(), defaultGsonProvider,
					configuredVersionOnBackendCreationOptional
			);
			clientFactoryHolder.close(); // We won't need it anymore

			elasticsearchVersion = initVersion( propertySource );

			ElasticsearchProtocolDialect protocolDialect = dialectFactory.createProtocolDialect( elasticsearchVersion );
			gsonProvider = GsonProvider.create( GsonBuilder::new, logPrettyPrinting );
			indexMetadataSyntax = protocolDialect.createIndexMetadataSyntax();
			searchSyntax = protocolDialect.createSearchSyntax();
			workFactory = protocolDialect.createWorkFactory( gsonProvider, QUERY_SHARD_FAILURE_IGNORE.get( propertySource ) );
			searchResultExtractorFactory = protocolDialect.createSearchResultExtractorFactory();
			scrollTimeout = SCROLL_TIMEOUT.get( propertySource );
		}
		indexLayoutStrategyHolder = createIndexLayoutStrategy( beanResolver, propertySource );
		ProjectionExtractionHelper<String> projectionExtractionHelper =
				typeNameMapping.onStart( indexLayoutStrategyHolder.get() );
		searchProjectionBackendContext = new SearchProjectionBackendContext(
				projectionExtractionHelper,
				multiTenancyStrategy.idProjectionExtractionHelper()
		);
	}

	void onStop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( BeanHolder::close, clientFactoryHolder ); // Just in case start() was not called
			closer.push( ElasticsearchClientImplementor::close, clientImplementor );
			closer.push( BeanHolder::close, indexLayoutStrategyHolder );
		}
	}

	private void checkStarted() {
		if ( clientImplementor == null ) {
			throw new AssertionFailure(
					"Attempt to retrieve Elasticsearch client or related information before the Elasticsearch client was started."
			);
		}
	}

	private ElasticsearchVersion initVersion(ConfigurationPropertySource propertySource) {
		Optional<Boolean> versionCheckEnabled = VERSION_CHECK_ENABLED.get( propertySource );
		Optional<ElasticsearchVersion> configuredVersionOptional = VERSION.getAndTransform( propertySource,
				configuredVersionOnStartOptional -> {
					Optional<ElasticsearchVersion> resultOptional;
					if ( configuredVersionOnStartOptional.isPresent() ) {
						// Allow overriding the version on start,
						// but expect it to match the version configured on backend creation (if any)
						if ( configuredVersionOnBackendCreationOptional.isPresent()
								&& !configuredVersionOnBackendCreationOptional.get()
										.matches( configuredVersionOnStartOptional.get() ) ) {
							throw VersionLog.INSTANCE.incompatibleElasticsearchVersionOnStart(
									configuredVersionOnBackendCreationOptional.get(),
									configuredVersionOnStartOptional.get() );
						}
						resultOptional = configuredVersionOnStartOptional;
					}
					else {
						// Default to the version configured when the backend was created
						resultOptional = configuredVersionOnBackendCreationOptional;
					}

					// If the version is unset or imprecise,
					// we will need to retrieve it from the cluster through a version check.
					// So in that situation, if version checks are disabled explicitly (they're enabled by default),
					// we'll raise an exception now, in the context of the "version" configuration property.
					if ( ( resultOptional.isEmpty()
							|| !ElasticsearchDialectFactory.isPreciseEnoughForProtocolDialect( resultOptional.get() ) )
							&& versionCheckEnabled.isPresent() && !versionCheckEnabled.get() ) {
						throw VersionLog.INSTANCE.impreciseElasticsearchVersionWhenVersionCheckDisabled(
								VERSION_CHECK_ENABLED.resolveOrRaw( propertySource ) );
					}

					return resultOptional;
				} );

		// If someone tries to force the version check on a distribution that doesn't support them
		// (Amazon OpenSearch Serverless), we'll raise an exception.
		boolean versionCheckImpossible = configuredVersionOptional.isPresent()
				&& ElasticsearchDialectFactory.isVersionCheckImpossible( configuredVersionOptional.get() );
		if ( versionCheckImpossible && versionCheckEnabled.isPresent() && versionCheckEnabled.get() ) {
			// Get the configuration property again in order to produce
			// an error message in the context of the problematic configuration property.
			VERSION_CHECK_ENABLED.getAndMap( propertySource, enabled -> {
				if ( enabled ) {
					throw VersionLog.INSTANCE.cannotCheckElasticsearchVersion( configuredVersionOptional.get().distribution() );
				}
				return enabled;
			} );
		}

		// Version checks are disabled by default if we know they're impossible.
		if ( versionCheckEnabled.orElse( !versionCheckImpossible ) ) {
			ElasticsearchVersion versionFromCluster = fetchElasticsearchVersion( propertySource );
			if ( configuredVersionOptional.isPresent() ) {
				ElasticsearchVersion configuredVersion = configuredVersionOptional.get();
				if ( !configuredVersion.matches( versionFromCluster ) ) {
					throw VersionLog.INSTANCE.unexpectedElasticsearchVersion( configuredVersion, versionFromCluster );
				}
			}
			return versionFromCluster;
		}
		else {
			// In this case we know the optional is non-empty:
			// see the checks when retrieving the configured version.
			return configuredVersionOptional.get();
		}
	}

	private ElasticsearchVersion fetchElasticsearchVersion(ConfigurationPropertySource propertySource) {
		try {
			ElasticsearchVersion version = ElasticsearchClientUtils.tryGetElasticsearchVersion( clientImplementor );
			if ( version == null ) {
				// This can happen when targeting Amazon OpenSearch Service
				// and we didn't notice the problem early
				// because the version was unset
				// or the distribution was incorrectly set to elasticsearch/opensearch.
				throw VersionLog.INSTANCE.unableToFetchElasticsearchVersion( VERSION.resolveOrRaw( propertySource ),
						ElasticsearchDialectFactory.AMAZON_OPENSEARCH_SERVERLESS );
			}
			return version;
		}
		catch (RuntimeException e) {
			throw VersionLog.INSTANCE.failedToDetectElasticsearchVersion( e.getMessage(), e );
		}
	}

	private BeanHolder<? extends IndexLayoutStrategy> createIndexLayoutStrategy(BeanResolver beanResolver,
			ConfigurationPropertySource propertySource) {
		return LAYOUT_STRATEGY.getAndTransform( propertySource, beanResolver::resolve );
	}
}

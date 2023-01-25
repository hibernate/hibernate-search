/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.session.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.plan.synchronization.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.plan.synchronization.impl.ConfiguredIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.session.spi.AbstractPojoSearchSession;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.common.EntityReference;
import org.hibernate.search.mapper.pojo.standalone.common.impl.EntityReferenceImpl;
import org.hibernate.search.mapper.pojo.standalone.loading.dsl.SelectionLoadingOptionsStep;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoLoadingContext;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoLoadingSessionContext;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoSelectionLoadingContextBuilder;
import org.hibernate.search.mapper.pojo.standalone.logging.impl.Log;
import org.hibernate.search.mapper.pojo.standalone.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.standalone.massindexing.impl.StandalonePojoMassIndexingSessionContext;
import org.hibernate.search.mapper.pojo.standalone.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.pojo.standalone.scope.SearchScope;
import org.hibernate.search.mapper.pojo.standalone.scope.impl.SearchScopeImpl;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSessionBuilder;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexer;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.standalone.work.SearchWorkspace;
import org.hibernate.search.mapper.pojo.standalone.work.impl.SearchIndexerImpl;
import org.hibernate.search.mapper.pojo.standalone.work.impl.SearchIndexingPlanImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class StandalonePojoSearchSession extends AbstractPojoSearchSession
		implements SearchSession, StandalonePojoMassIndexingSessionContext, StandalonePojoLoadingSessionContext,
				DocumentReferenceConverter<EntityReference> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final StandalonePojoSearchSessionMappingContext mappingContext;
	private final StandalonePojoSearchSessionTypeContextProvider typeContextProvider;

	private final String tenantId;

	private final Consumer<SelectionLoadingOptionsStep> loadingOptionsContributor;

	private SearchIndexingPlanImpl indexingPlan;
	private SearchIndexer indexer;
	private boolean open = true;
	private ConfiguredIndexingPlanSynchronizationStrategy<EntityReference> indexingPlanSynchronizationStrategy;

	private StandalonePojoSearchSession(Builder builder) {
		super( builder.mappingContext );
		this.mappingContext = builder.mappingContext;
		this.typeContextProvider = builder.typeContextProvider;
		this.tenantId = builder.tenantId;
		this.loadingOptionsContributor = builder.loadingOptionsContributor;

		this.indexingPlanSynchronizationStrategy = configureSynchronizationStrategy(
				builder.resolveAutomaticIndexingSynchronizationStrategy()
		);
	}

	private void checkOpenAndThrow() {
		if ( !open ) {
			throw log.hibernateSessionAccessError( "is closed" );
		}
	}

	@Override
	public void close() {
		if ( !open ) {
			return;
		}
		open = false;
		if ( indexingPlan != null ) {
			indexingPlan.execute();
		}
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public MassIndexer massIndexer(Collection<? extends Class<?>> types) {
		checkOpenAndThrow();
		return scope( types ).massIndexer( Collections.singletonList( this.tenantIdentifier() ) );
	}

	@Override
	public String tenantIdentifier() {
		return tenantId;
	}

	@Override
	public void indexingPlanSynchronizationStrategy( IndexingPlanSynchronizationStrategy synchronizationStrategy) {
		this.indexingPlanSynchronizationStrategy = configureSynchronizationStrategy( synchronizationStrategy );
	}

	@Override
	public PojoIndexer createIndexer() {
		return mappingContext.createIndexer( this );
	}

	@Override
	public PojoRuntimeIntrospector runtimeIntrospector() {
		return mappingContext.runtimeIntrospector();
	}

	@Override
	public <T> SearchQuerySelectStep<?, EntityReference, T, ?, ?, ?> search(Collection<? extends Class<? extends T>> types) {
		return search( scope( types ) );
	}

	@Override
	public <T> SearchQuerySelectStep<?, EntityReference, T, ?, ?, ?> search(SearchScope<T> scope) {
		return search( (SearchScopeImpl<T>) scope );
	}

	@Override
	public SearchSchemaManager schemaManager(Collection<? extends Class<?>> types) {
		return scope( types ).schemaManager();
	}

	@Override
	public SearchWorkspace workspace(Collection<? extends Class<?>> types) {
		return scope( types ).workspace( tenantIdentifier() );
	}

	@Override
	public <T> SearchScopeImpl<T> scope(Collection<? extends Class<? extends T>> types) {
		return mappingContext.createScope( types );
	}

	@Override
	public SearchIndexingPlan indexingPlan() {
		if ( indexingPlan == null ) {
			indexingPlan = new SearchIndexingPlanImpl(
					typeContextProvider, runtimeIntrospector(),
					mappingContext().createIndexingPlan(
							this,
							indexingPlanSynchronizationStrategy.getDocumentCommitStrategy(),
							indexingPlanSynchronizationStrategy.getDocumentRefreshStrategy()
					),
					indexingPlanSynchronizationStrategy
			);
		}
		return indexingPlan;
	}

	@Override
	public SearchIndexer indexer() {
		if ( indexer == null ) {
			indexer = new SearchIndexerImpl(
					runtimeIntrospector(),
					mappingContext().createIndexer( this ),
					indexingPlanSynchronizationStrategy.getDocumentCommitStrategy(),
					indexingPlanSynchronizationStrategy.getDocumentRefreshStrategy()
			);
		}
		return indexer;
	}

	@Override
	public EntityReference fromDocumentReference(DocumentReference reference) {
		StandalonePojoSessionIndexedTypeContext<?> typeContext =
				typeContextProvider.indexedByEntityName().getOrFail( reference.typeName() );
		Object id = typeContext.identifierMapping()
				.fromDocumentIdentifier( reference.id(), this );
		return new EntityReferenceImpl( typeContext.typeIdentifier(), typeContext.name(), id );
	}

	@Override
	public PojoSelectionLoadingContext defaultLoadingContext() {
		return loadingContextBuilder().build();
	}

	@Override
	public StandalonePojoSearchSessionMappingContext mappingContext() {
		return mappingContext;
	}

	private <T> SearchQuerySelectStep<?, EntityReference, T, ?, ?, ?> search(SearchScopeImpl<T> scope) {
		return scope.search( this, this, loadingContextBuilder() );
	}

	private StandalonePojoSelectionLoadingContextBuilder loadingContextBuilder() {
		StandalonePojoLoadingContext.Builder builder = mappingContext.loadingContextBuilder();
		if ( loadingOptionsContributor != null ) {
			loadingOptionsContributor.accept( builder );
		}
		return builder;
	}

	private ConfiguredIndexingPlanSynchronizationStrategy<EntityReference> configureSynchronizationStrategy(
			IndexingPlanSynchronizationStrategy synchronizationStrategy) {
		ConfiguredIndexingPlanSynchronizationStrategy.Builder<EntityReference> builder =
				new ConfiguredIndexingPlanSynchronizationStrategy.Builder<>(
						mappingContext.failureHandler(),
						mappingContext.entityReferenceFactory()
				);
		synchronizationStrategy.apply( builder );
		return builder.build();
	}

	public static class Builder implements SearchSessionBuilder {

		private static final OptionalConfigurationProperty<BeanReference<? extends IndexingPlanSynchronizationStrategy>> AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY =
				ConfigurationProperty.forKey( StandalonePojoMapperSettings.Radicals.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY )
						.asBeanReference( IndexingPlanSynchronizationStrategy.class )
						.build();

		private final ConfigurationPropertySource configurationPropertySource;
		private final BeanResolver beanResolver;
		private final StandalonePojoSearchSessionMappingContext mappingContext;
		private final StandalonePojoSearchSessionTypeContextProvider typeContextProvider;

		private IndexingPlanSynchronizationStrategy synchronizationStrategy;

		private String tenantId;
		private DocumentCommitStrategy commitStrategy;
		private DocumentRefreshStrategy refreshStrategy;
		private Consumer<SelectionLoadingOptionsStep> loadingOptionsContributor;

		public Builder(StandalonePojoSearchSessionMappingContext mappingContext,
				ConfigurationPropertySource configurationPropertySource, BeanResolver beanResolver,
				StandalonePojoSearchSessionTypeContextProvider typeContextProvider) {
			this.configurationPropertySource = configurationPropertySource;
			this.beanResolver = beanResolver;
			this.mappingContext = mappingContext;
			this.typeContextProvider = typeContextProvider;
		}

		@Override
		public Builder tenantId(String tenantId) {
			this.tenantId = tenantId;
			return this;
		}

		@SuppressWarnings( "deprecation" ) // need to keep OLD API still implemented
		@Override
		public SearchSessionBuilder commitStrategy(DocumentCommitStrategy commitStrategy) {
			this.commitStrategy = commitStrategy;
			return this;
		}

		@SuppressWarnings( "deprecation" ) // need to keep OLD API still implemented
		@Override
		public SearchSessionBuilder refreshStrategy(DocumentRefreshStrategy refreshStrategy) {
			this.refreshStrategy = refreshStrategy;
			return this;
		}

		@Override
		public SearchSessionBuilder indexingPlanSynchronizationStrategy(IndexingPlanSynchronizationStrategy synchronizationStrategy) {
			this.synchronizationStrategy = synchronizationStrategy;
			return this;
		}

		@Override
		public SearchSessionBuilder loading(Consumer<SelectionLoadingOptionsStep> loadingOptionsContributor) {
			this.loadingOptionsContributor = loadingOptionsContributor;
			return this;
		}

		@Override
		public StandalonePojoSearchSession build() {
			boolean lowLevelConfig = ( refreshStrategy != null || commitStrategy != null );
			if ( synchronizationStrategy != null && lowLevelConfig ) {
				throw log.conflictingIndexPlanSynchronizationConfiguration();
			}
			else if ( synchronizationStrategy == null && lowLevelConfig ) {
				synchronizationStrategy = strategy(
						Optional.ofNullable( commitStrategy ).orElse( DocumentCommitStrategy.FORCE ),
						Optional.ofNullable( refreshStrategy ).orElse( DocumentRefreshStrategy.NONE )
				);
			}
			else if ( synchronizationStrategy == null ) {
				synchronizationStrategy = IndexingPlanSynchronizationStrategy.writeSync();
			}

			return new StandalonePojoSearchSession( this );
		}

		private IndexingPlanSynchronizationStrategy resolveAutomaticIndexingSynchronizationStrategy() {
			try ( BeanHolder<? extends IndexingPlanSynchronizationStrategy> beanHolder = AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY.getAndTransform(
					configurationPropertySource,
					referenceOptional -> beanResolver.resolve( referenceOptional.orElse(
							StandalonePojoMapperSettings.Defaults.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY ) )
			) ) {
				return synchronizationStrategy != null ? synchronizationStrategy :
						beanHolder.get();
			}
		}

		private static IndexingPlanSynchronizationStrategy strategy(
				DocumentCommitStrategy commitStrategy,
				DocumentRefreshStrategy refreshStrategy
		) {
			if ( DocumentCommitStrategy.NONE.equals( commitStrategy ) && DocumentRefreshStrategy.NONE.equals(
					refreshStrategy ) ) {
				return IndexingPlanSynchronizationStrategy.async();
			}
			if ( DocumentCommitStrategy.FORCE.equals( commitStrategy ) && DocumentRefreshStrategy.FORCE.equals(
					refreshStrategy ) ) {
				return IndexingPlanSynchronizationStrategy.sync();
			}
			if ( DocumentCommitStrategy.FORCE.equals( commitStrategy ) && DocumentRefreshStrategy.NONE.equals(
					refreshStrategy ) ) {
				return IndexingPlanSynchronizationStrategy.writeSync();
			}
			if ( DocumentCommitStrategy.NONE.equals( commitStrategy ) && DocumentRefreshStrategy.FORCE.equals(
					refreshStrategy ) ) {
				return IndexingPlanSynchronizationStrategy.readSync();
			}
			throw new IllegalStateException( "This shouldn't happen." );
		}
	}
}

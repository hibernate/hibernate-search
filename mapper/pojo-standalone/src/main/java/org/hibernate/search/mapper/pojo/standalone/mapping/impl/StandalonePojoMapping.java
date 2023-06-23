/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.reporting.spi.BackendMappingHints;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.mapper.mapping.spi.MappingPreStopContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappingStartContext;
import org.hibernate.search.engine.search.projection.spi.ProjectionMappedTypeContext;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingImplementor;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgent;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgentCreateContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoLoadingContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.massindexing.impl.StandalonePojoMassIndexingSessionContext;
import org.hibernate.search.mapper.pojo.standalone.reporting.impl.StandalonePojoMappingHints;
import org.hibernate.search.mapper.pojo.standalone.schema.management.impl.SchemaManagementListener;
import org.hibernate.search.mapper.pojo.standalone.scope.SearchScope;
import org.hibernate.search.mapper.pojo.standalone.scope.impl.SearchScopeImpl;
import org.hibernate.search.mapper.pojo.standalone.scope.impl.StandalonePojoScopeIndexedTypeContext;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSessionBuilder;
import org.hibernate.search.mapper.pojo.standalone.session.impl.StandalonePojoSearchSession;
import org.hibernate.search.mapper.pojo.standalone.session.impl.StandalonePojoSearchSessionMappingContext;
import org.hibernate.search.util.common.impl.Closer;

public class StandalonePojoMapping extends AbstractPojoMappingImplementor<StandalonePojoMapping>
		implements CloseableSearchMapping, StandalonePojoSearchSessionMappingContext {

	private final StandalonePojoTypeContextContainer typeContextContainer;
	private final SchemaManagementListener schemaManagementListener;
	private final ConfiguredIndexingPlanSynchronizationStrategyHolder configuredIndexingPlanSynchronizationStrategyHolder;

	private SearchIntegration.Handle integrationHandle;
	private boolean active;


	StandalonePojoMapping(PojoMappingDelegate mappingDelegate, StandalonePojoTypeContextContainer typeContextContainer,
			SchemaManagementListener schemaManagementListener) {
		super( mappingDelegate );
		this.typeContextContainer = typeContextContainer;
		this.schemaManagementListener = schemaManagementListener;
		this.configuredIndexingPlanSynchronizationStrategyHolder = new ConfiguredIndexingPlanSynchronizationStrategyHolder(
				this );
		this.active = true;
	}

	@Override
	public CompletableFuture<?> start(MappingStartContext context) {
		integrationHandle = context.integrationHandle();

		configuredIndexingPlanSynchronizationStrategyHolder.start( context );

		Optional<SearchScopeImpl<Object>> scopeOptional = createAllScope();
		if ( !scopeOptional.isPresent() ) {
			// No indexed type
			return CompletableFuture.completedFuture( null );
		}
		SearchScopeImpl<Object> scope = scopeOptional.get();

		// Schema management
		PojoScopeSchemaManager schemaManager = scope.schemaManagerDelegate();

		return schemaManagementListener.onStart( context, schemaManager );
	}

	@Override
	public CompletableFuture<?> preStop(MappingPreStopContext context) {
		Optional<SearchScopeImpl<Object>> scope = createAllScope();
		if ( !scope.isPresent() ) {
			// No indexed type
			return CompletableFuture.completedFuture( null );
		}
		PojoScopeSchemaManager schemaManager = scope.get().schemaManagerDelegate();
		return schemaManagementListener.onStop( context, schemaManager );
	}

	@Override
	public void close() {
		if ( !active ) {
			return;
		}
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( SearchIntegration::close, integrationHandle, SearchIntegration.Handle::getOrNull );
			closer.push(
					ConfiguredIndexingPlanSynchronizationStrategyHolder::close,
					configuredIndexingPlanSynchronizationStrategyHolder
			);
			integrationHandle = null;
			active = false;
		}
	}

	@Override
	public BackendMappingHints hints() {
		return StandalonePojoMappingHints.INSTANCE;
	}

	@Override
	public ProjectionMappedTypeContext mappedTypeContext(String mappedTypeName) {
		return typeContextContainer.indexedByEntityName().getOrFail( mappedTypeName );
	}

	@Override
	public PojoRuntimeIntrospector runtimeIntrospector() {
		return PojoRuntimeIntrospector.simple();
	}

	@Override
	public <T> SearchScope<T> scope(Collection<? extends Class<? extends T>> targetedTypes) {
		return createScope( targetedTypes );
	}

	@Override
	public StandalonePojoMapping toConcreteType() {
		return this;
	}

	@Override
	public SearchSession createSession() {
		return createSessionBuilder().build();
	}

	@Override
	public SearchSessionBuilder createSessionWithOptions() {
		return createSessionBuilder();
	}

	@Override
	public <T> SearchScopeImpl<T> createScope(Collection<? extends Class<? extends T>> classes) {
		List<PojoRawTypeIdentifier<? extends T>> typeIdentifiers = new ArrayList<>( classes.size() );
		for ( Class<? extends T> clazz : classes ) {
			typeIdentifiers.add( PojoRawTypeIdentifier.of( clazz ) );
		}

		// Explicit type parameter is necessary here for ECJ (Eclipse compiler)
		return new SearchScopeImpl<T>( this,
				delegate().createPojoScope( this, typeIdentifiers,
						typeContextContainer::indexedForExactType ) );
	}

	@Override
	public <E> SearchIndexedEntity<E> indexedEntity(Class<E> entityType) {
		return typeContextContainer.indexedForExactClass( entityType );
	}

	@Override
	public SearchIndexedEntity<?> indexedEntity(String entityName) {
		return typeContextContainer.indexedByEntityName().getOrFail( entityName );
	}

	@Override
	public Collection<SearchIndexedEntity<?>> allIndexedEntities() {
		return Collections.unmodifiableCollection( typeContextContainer.allIndexed() );
	}

	@Override
	public IndexManager indexManager(String indexName) {
		return searchIntegration().indexManager( indexName );
	}

	@Override
	public Backend backend() {
		return searchIntegration().backend();
	}

	@Override
	public Backend backend(String backendName) {
		return searchIntegration().backend( backendName );
	}

	@Override
	public StandalonePojoLoadingContext.Builder loadingContextBuilder() {
		return new StandalonePojoLoadingContext.Builder( this, typeContextContainer );
	}

	@Override
	public PojoMassIndexerAgent createMassIndexerAgent(PojoMassIndexerAgentCreateContext context) {
		// No coordination: so we don't need to prevent outbox-polling event processing (since it's not supported) when doing mass-indexing.
		return PojoMassIndexerAgent.noOp();
	}

	@Override
	public StandalonePojoMassIndexingSessionContext createSession(String tenantIdentifier) {
		return createSessionBuilder().tenantId( tenantIdentifier ).build();
	}

	private SearchIntegration searchIntegration() {
		return integrationHandle.getOrFail();
	}

	private Optional<SearchScopeImpl<Object>> createAllScope() {
		return delegate()
				.<EntityReference, StandalonePojoScopeIndexedTypeContext<?>>createPojoAllScope(
						this,
						typeContextContainer::indexedForExactType
				)
				.map( scopeDelegate -> new SearchScopeImpl<>( this, scopeDelegate ) );
	}

	private StandalonePojoSearchSession.Builder createSessionBuilder() {
		return new StandalonePojoSearchSession.Builder(
				this, configuredIndexingPlanSynchronizationStrategyHolder, typeContextContainer
		);
	}
}

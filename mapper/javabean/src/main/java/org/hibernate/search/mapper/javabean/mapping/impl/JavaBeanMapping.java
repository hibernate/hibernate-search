/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.mapper.mapping.spi.MappingPreStopContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappingStartContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.javabean.common.EntityReference;
import org.hibernate.search.mapper.javabean.common.impl.EntityReferenceImpl;
import org.hibernate.search.mapper.javabean.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.javabean.loading.impl.JavaBeanLoadingContext;
import org.hibernate.search.mapper.javabean.log.impl.Log;
import org.hibernate.search.mapper.javabean.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.massindexing.impl.JavaBeanMassIndexingSessionContext;
import org.hibernate.search.mapper.javabean.schema.management.impl.SchemaManagementListener;
import org.hibernate.search.mapper.javabean.scope.SearchScope;
import org.hibernate.search.mapper.javabean.scope.impl.JavaBeanScopeIndexedTypeContext;
import org.hibernate.search.mapper.javabean.scope.impl.SearchScopeImpl;
import org.hibernate.search.mapper.javabean.session.SearchSessionBuilder;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.javabean.session.impl.JavaBeanSearchSession;
import org.hibernate.search.mapper.javabean.session.impl.JavaBeanSearchSessionMappingContext;
import org.hibernate.search.mapper.javabean.session.impl.JavaBeanSessionIndexedTypeContext;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingImplementor;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgent;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgentCreateContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class JavaBeanMapping extends AbstractPojoMappingImplementor<SearchMapping>
		implements CloseableSearchMapping, JavaBeanSearchSessionMappingContext, EntityReferenceFactory<EntityReference> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final JavaBeanTypeContextContainer typeContextContainer;

	private SearchIntegration integration;

	private boolean active;

	private final SchemaManagementListener schemaManagementListener;

	JavaBeanMapping(PojoMappingDelegate mappingDelegate, JavaBeanTypeContextContainer typeContextContainer,
			SchemaManagementListener schemaManagementListener) {
		super( mappingDelegate );
		this.typeContextContainer = typeContextContainer;
		this.schemaManagementListener = schemaManagementListener;
		this.active = true;
	}

	@Override
	public CompletableFuture<?> start(MappingStartContext context) {
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
		try {
			if ( integration != null ) {
				integration.close();
			}
		}
		finally {
			active = false;
		}
	}

	@Override
	public EntityReferenceFactory<EntityReference> entityReferenceFactory() {
		return this;
	}

	@Override
	public PojoRuntimeIntrospector runtimeIntrospector() {
		return PojoRuntimeIntrospector.simple();
	}

	@Override
	public EntityReference createEntityReference(String typeName, Object identifier) {
		JavaBeanSessionIndexedTypeContext<?> typeContext
				= typeContextContainer.indexedForEntityName( typeName );
		if ( typeContext == null ) {
			throw new AssertionFailure(
					"Type name " + typeName + " refers to an unknown type"
			);
		}
		return new EntityReferenceImpl( typeContext.typeIdentifier(), typeContext.name(), identifier );
	}

	@Override
	public <T> SearchScope<T> scope(Collection<? extends Class<? extends T>> targetedTypes) {
		return createScope( targetedTypes );
	}

	@Override
	public SearchMapping toConcreteType() {
		return this;
	}

	@Override
	public SearchSession createSession() {
		return createSearchManagerBuilder().build();
	}

	@Override
	public SearchSessionBuilder createSessionWithOptions() {
		return createSearchManagerBuilder();
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
		SearchIndexedEntity<E> type = typeContextContainer.indexedForExactClass( entityType );
		if ( type == null ) {
			throw log.notIndexedEntityType( entityType );
		}
		return type;
	}

	@Override
	public SearchIndexedEntity<?> indexedEntity(String entityName) {
		SearchIndexedEntity<?> type = typeContextContainer.indexedForEntityName( entityName );
		if ( type == null ) {
			throw log.notIndexedEntityName( entityName );
		}
		return type;
	}

	@Override
	public Collection<SearchIndexedEntity<?>> allIndexedEntities() {
		return Collections.unmodifiableCollection( typeContextContainer.allIndexed() );
	}

	@Override
	public JavaBeanLoadingContext.Builder loadingContextBuilder(DetachedBackendSessionContext sessionContext) {
		return new JavaBeanLoadingContext.Builder( this, typeContextContainer, sessionContext );
	}

	@Override
	public ThreadPoolProvider threadPoolProvider() {
		return delegate().threadPoolProvider();
	}

	@Override
	public FailureHandler failureHandler() {
		return delegate().failureHandler();
	}

	@Override
	public PojoMassIndexerAgent createMassIndexerAgent(PojoMassIndexerAgentCreateContext context) {
		// No coordination: we don't prevent automatic indexing from continuing while mass indexing.
		return PojoMassIndexerAgent.noOp();
	}

	@Override
	public JavaBeanMassIndexingSessionContext createSession(DetachedBackendSessionContext sessionContext) {
		return createSearchManagerBuilder().tenantId( sessionContext.tenantIdentifier() ).build();
	}

	@Override
	public DetachedBackendSessionContext detachedBackendSessionContext(String tenantId) {
		return DetachedBackendSessionContext.of( this, tenantId );
	}

	public void setIntegration(SearchIntegration integration) {
		this.integration = integration;
	}

	private Optional<SearchScopeImpl<Object>> createAllScope() {
		return delegate()
				.<EntityReference, JavaBeanScopeIndexedTypeContext<?>>createPojoAllScope(
						this,
						typeContextContainer::indexedForExactType
				)
				.map( scopeDelegate -> new SearchScopeImpl<>( this, scopeDelegate ) );
	}

	private JavaBeanSearchSession.Builder createSearchManagerBuilder() {
		return new JavaBeanSearchSession.Builder(
				this, typeContextContainer
		);
	}
}

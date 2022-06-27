/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.session.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.mapper.pojo.standalone.common.EntityReference;
import org.hibernate.search.mapper.pojo.standalone.loading.dsl.SelectionLoadingOptionsStep;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoLoadingSessionContext;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoLoadingContext;
import org.hibernate.search.mapper.pojo.standalone.scope.SearchScope;
import org.hibernate.search.mapper.pojo.standalone.scope.impl.SearchScopeImpl;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSessionBuilder;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexer;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.standalone.work.SearchWorkspace;
import org.hibernate.search.mapper.pojo.standalone.work.impl.SearchIndexerImpl;
import org.hibernate.search.mapper.pojo.standalone.work.impl.SearchIndexingPlanImpl;
import org.hibernate.search.mapper.pojo.standalone.common.impl.EntityReferenceImpl;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoSelectionLoadingContextBuilder;
import org.hibernate.search.mapper.pojo.standalone.log.impl.Log;
import org.hibernate.search.mapper.pojo.standalone.massindexing.impl.StandalonePojoMassIndexingSessionContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.session.spi.AbstractPojoSearchSession;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.mapper.pojo.standalone.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.standalone.schema.management.SearchSchemaManager;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class StandalonePojoSearchSession extends AbstractPojoSearchSession
		implements SearchSession, StandalonePojoMassIndexingSessionContext, StandalonePojoLoadingSessionContext,
				DocumentReferenceConverter<EntityReference> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final StandalonePojoSearchSessionMappingContext mappingContext;
	private final StandalonePojoSearchSessionTypeContextProvider typeContextProvider;

	private final String tenantId;

	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;
	private final Consumer<SelectionLoadingOptionsStep> loadingOptionsContributor;

	private SearchIndexingPlanImpl indexingPlan;
	private SearchIndexer indexer;
	private boolean open = true;

	private StandalonePojoSearchSession(Builder builder) {
		super( builder.mappingContext );
		this.mappingContext = builder.mappingContext;
		this.typeContextProvider = builder.typeContextProvider;
		this.tenantId = builder.tenantId;
		this.commitStrategy = builder.commitStrategy;
		this.refreshStrategy = builder.refreshStrategy;
		this.loadingOptionsContributor = builder.loadingOptionsContributor;
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
			CompletableFuture<?> future = indexingPlan.execute();
			Futures.unwrappedExceptionJoin( future );
		}
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public MassIndexer massIndexer(Collection<? extends Class<?>> types) {
		checkOpenAndThrow();
		return scope( types ).massIndexer( DetachedBackendSessionContext.of( this ) );
	}

	@Override
	public String tenantIdentifier() {
		return tenantId;
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
		return scope( types ).workspace( DetachedBackendSessionContext.of( this ) );
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
					mappingContext().createIndexingPlan( this, commitStrategy, refreshStrategy ),
					mappingContext.entityReferenceFactory()
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
					commitStrategy, refreshStrategy
			);
		}
		return indexer;
	}

	@Override
	public EntityReference fromDocumentReference(DocumentReference reference) {
		StandalonePojoSessionIndexedTypeContext<?> typeContext =
				typeContextProvider.indexedForEntityName( reference.typeName() );
		if ( typeContext == null ) {
			throw new AssertionFailure(
					"Document reference " + reference + " refers to an unknown type"
			);
		}
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
		StandalonePojoLoadingContext.Builder builder = mappingContext.loadingContextBuilder( DetachedBackendSessionContext.of( this ) );
		if ( loadingOptionsContributor != null ) {
			loadingOptionsContributor.accept( builder );
		}
		return builder;
	}

	public static class Builder
			implements SearchSessionBuilder {
		private final StandalonePojoSearchSessionMappingContext mappingContext;
		private final StandalonePojoSearchSessionTypeContextProvider typeContextProvider;
		private String tenantId;
		private DocumentCommitStrategy commitStrategy = DocumentCommitStrategy.FORCE;
		private DocumentRefreshStrategy refreshStrategy = DocumentRefreshStrategy.NONE;
		private Consumer<SelectionLoadingOptionsStep> loadingOptionsContributor;

		public Builder(StandalonePojoSearchSessionMappingContext mappingContext,
				StandalonePojoSearchSessionTypeContextProvider typeContextProvider) {
			this.mappingContext = mappingContext;
			this.typeContextProvider = typeContextProvider;
		}

		@Override
		public Builder tenantId(String tenantId) {
			this.tenantId = tenantId;
			return this;
		}

		@Override
		public SearchSessionBuilder commitStrategy(DocumentCommitStrategy commitStrategy) {
			this.commitStrategy = commitStrategy;
			return this;
		}

		@Override
		public SearchSessionBuilder refreshStrategy(DocumentRefreshStrategy refreshStrategy) {
			this.refreshStrategy = refreshStrategy;
			return this;
		}

		@Override
		public SearchSessionBuilder loading(Consumer<SelectionLoadingOptionsStep> loadingOptionsContributor) {
			this.loadingOptionsContributor = loadingOptionsContributor;
			return this;
		}

		@Override
		public StandalonePojoSearchSession build() {
			return new StandalonePojoSearchSession( this );
		}
	}
}

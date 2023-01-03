/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import java.util.Optional;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.util.common.AssertionFailure;

/**
 * A wrapper around {@link MappedIndexManager} providing some syntactic sugar,
 * such as methods that do not force to provide a session context.
 */
public abstract class StubMappedIndex {

	public static StubMappedIndex withoutFields() {
		return ofAdvancedNonRetrievable( ignored -> { } );
	}

	public static StubMappedIndex ofNonRetrievable(Consumer<? super IndexSchemaElement> binder) {
		return ofAdvancedNonRetrievable( ctx -> binder.accept( ctx.schemaElement() ) );
	}

	public static StubMappedIndex ofAdvancedNonRetrievable(Consumer<? super IndexedEntityBindingContext> binder) {
		return new StubMappedIndex() {
			@Override
			protected void bind(IndexedEntityBindingContext context) {
				binder.accept( context );
			}
		};
	}

	private String indexName;
	private String typeName;
	private String backendName;
	private MappedIndexManager manager;
	private StubMapping mapping;

	public StubMappedIndex() {
		this.indexName = "indexName";
		this.typeName = null;
	}

	public StubMapping mapping() {
		return mapping;
	}

	public final String name() {
		return indexName;
	}

	public StubMappedIndex name(String name) {
		this.indexName = name;
		return this;
	}

	public final String typeName() {
		return typeName != null ? typeName : indexName + "Type";
	}

	public StubMappedIndex typeName(String name) {
		this.typeName = name;
		return this;
	}

	public final Optional<String> backendName() {
		return Optional.ofNullable( backendName );
	}

	public StubMappedIndex backendName(String name) {
		this.backendName = name;
		return this;
	}

	public IndexManager toApi() {
		return manager.toAPI();
	}

	public <T> T unwrapForTests(Class<T> clazz) {
		return clazz.cast( delegate().toAPI() );
	}

	public IndexSchemaManager schemaManager() {
		return delegate().schemaManager();
	}

	/**
	 * Execute indexing for the given document, without waiting for the change to be committed.
	 * <p>
	 * Used to test indexing of specific data in tests that are not specifically about indexing.
	 */
	public void index(String id, DocumentContributor documentContributor) {
		bulkIndexer().add( id, documentContributor ).join();
	}

	public BulkIndexer bulkIndexer() {
		return bulkIndexer( mapping.session(), true );
	}

	public BulkIndexer bulkIndexer(boolean refresh) {
		return bulkIndexer( mapping.session(), refresh );
	}

	public BulkIndexer bulkIndexer(StubSession sessionContext, boolean refresh) {
		return new BulkIndexer( delegate(), sessionContext, refresh );
	}

	public IndexIndexingPlan createIndexingPlan() {
		return createIndexingPlan( mapping.session() );
	}

	public IndexIndexingPlan createIndexingPlan(String tenantId) {
		return createIndexingPlan( mapping.session( tenantId ) );
	}

	public IndexIndexingPlan createIndexingPlan(StubSession sessionContext) {
		/*
		 * Use the same defaults as in the ORM mapper for the commit strategy,
		 * but force refreshes because it's more convenient for tests.
		 */
		return delegate().createIndexingPlan( sessionContext,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE );
	}

	public IndexIndexingPlan createIndexingPlan(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return delegate().createIndexingPlan( mapping.session(),
				commitStrategy, refreshStrategy );
	}

	public IndexIndexingPlan createIndexingPlan(StubSession sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return delegate().createIndexingPlan( sessionContext,
				commitStrategy, refreshStrategy );
	}

	public IndexIndexer createIndexer() {
		return createIndexer( mapping.session() );
	}

	public IndexIndexer createIndexer(StubSession sessionContext) {
		return delegate().createIndexer( sessionContext );
	}

	public IndexWorkspace createWorkspace() {
		return createWorkspace( mapping.session() );
	}

	public IndexWorkspace createWorkspace(StubSession sessionContext) {
		return createWorkspace( sessionContext.tenantIdentifier() );
	}

	public IndexWorkspace createWorkspace(String tenantId) {
		return delegate().createWorkspace( mapping, tenantId );
	}

	/**
	 * @return {@code createScope().query()}.
	 */
	public SearchQuerySelectStep<?, DocumentReference, DocumentReference, StubLoadingOptionsStep, ?, ?> query() {
		return createScope().query();
	}

	/**
	 * @return A scope containing this index only.
	 */
	public StubMappingScope createScope() {
		MappedIndexScopeBuilder<DocumentReference, DocumentReference> builder =
				delegate().createScopeBuilder( mapping );
		return new StubMappingScope( mapping, builder.build() );
	}

	/**
	 * @return A scope containing this index and the given other indexes.
	 */
	public StubMappingScope createScope(StubMappedIndex... others) {
		MappedIndexScopeBuilder<DocumentReference, DocumentReference> builder =
				delegate().createScopeBuilder( mapping );
		for ( StubMappedIndex other : others ) {
			other.delegate().addTo( builder );
		}
		return new StubMappingScope( mapping, builder.build() );
	}

	/**
	 * @return A scope containing this index and the given other indexes.
	 */
	public <R, E> GenericStubMappingScope<R, E> createGenericScope(
			SearchLoadingContext<R, E> loadingContext, StubMappedIndex... others) {
		if ( ( (StubMappingImpl) mapping ).fixture.typeContexts == null ) {
			throw new AssertionFailure( "When testing loading with a \"generic\" scope,"
					+ " you must also set custom type contexts with consistent types."
					+ " Use mapping.with().typeContext(...).run(...)." );
		}
		MappedIndexScopeBuilder<R, E> builder = delegate().createScopeBuilder( mapping );
		for ( StubMappedIndex other : others ) {
			other.delegate().addTo( builder );
		}
		return new GenericStubMappingScope<>( mapping, builder.build(), loadingContext );
	}

	protected abstract void bind(IndexedEntityBindingContext context);

	protected void onIndexManagerCreated(MappedIndexManager manager) {
		this.manager = manager;
	}

	protected void onMappingCreated(StubMapping mapping) {
		this.mapping = mapping;
	}

	protected MappedIndexManager delegate() {
		return manager;
	}
}

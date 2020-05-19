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
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;

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

	public StubMappedIndex() {
		this.indexName = "indexName";
		this.typeName = null;
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
		return bulkIndexer( new StubBackendSessionContext(), true );
	}

	public BulkIndexer bulkIndexer(boolean refresh) {
		return bulkIndexer( new StubBackendSessionContext(), refresh );
	}

	public BulkIndexer bulkIndexer(StubBackendSessionContext sessionContext, boolean refresh) {
		return new BulkIndexer( delegate(), sessionContext, refresh );
	}

	public IndexIndexingPlan<StubEntityReference> createIndexingPlan() {
		return createIndexingPlan( new StubBackendSessionContext() );
	}

	public IndexIndexingPlan<StubEntityReference> createIndexingPlan(StubBackendSessionContext sessionContext) {
		/*
		 * Use the same defaults as in the ORM mapper for the commit strategy,
		 * but force refreshes because it's more convenient for tests.
		 */
		return delegate().createIndexingPlan( sessionContext, StubEntityReference.FACTORY,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE );
	}

	public IndexIndexingPlan<StubEntityReference> createIndexingPlan(StubBackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return delegate().createIndexingPlan( sessionContext, StubEntityReference.FACTORY,
				commitStrategy, refreshStrategy );
	}

	public IndexIndexer createIndexer() {
		return createIndexer( new StubBackendSessionContext() );
	}

	public IndexIndexer createIndexer(StubBackendSessionContext sessionContext) {
		return delegate().createIndexer( sessionContext );
	}

	public IndexWorkspace createWorkspace() {
		return createWorkspace( new StubBackendSessionContext() );
	}

	public IndexWorkspace createWorkspace(StubBackendSessionContext sessionContext) {
		return createWorkspace( DetachedBackendSessionContext.of( sessionContext ) );
	}

	public IndexWorkspace createWorkspace(DetachedBackendSessionContext sessionContext) {
		return delegate().createWorkspace( sessionContext );
	}

	/**
	 * @return A scope containing this index only.
	 */
	public StubMappingScope createScope() {
		MappedIndexScopeBuilder<DocumentReference, DocumentReference> builder =
				delegate().createScopeBuilder( new StubBackendMappingContext() );
		return new StubMappingScope( builder.build() );
	}

	/**
	 * @return A scope containing this index and the given other indexes.
	 */
	public StubMappingScope createScope(StubMappedIndex... others) {
		MappedIndexScopeBuilder<DocumentReference, DocumentReference> builder =
				delegate().createScopeBuilder( new StubBackendMappingContext() );
		for ( StubMappedIndex other : others ) {
			other.delegate().addTo( builder );
		}
		return new StubMappingScope( builder.build() );
	}

	/**
	 * @return A scope containing this index and the given other indexes.
	 */
	public <R, E> GenericStubMappingScope<R, E> createGenericScope(StubMappedIndex... others) {
		MappedIndexScopeBuilder<R, E> builder =
				delegate().createScopeBuilder( new StubBackendMappingContext() );
		for ( StubMappedIndex other : others ) {
			other.delegate().addTo( builder );
		}
		return new GenericStubMappingScope<>( builder.build() );
	}

	protected abstract void bind(IndexedEntityBindingContext context);

	protected void onIndexManagerCreated(MappedIndexManager manager) {
		this.manager = manager;
	}

	protected MappedIndexManager delegate() {
		return manager;
	}
}

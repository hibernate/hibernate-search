/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.mapper;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkExecutor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubBackendMappingContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubBackendSessionContext;

/**
 * A wrapper around {@link MappedIndexManager} providing some syntactic sugar,
 * such as methods that do not force to provide a session context.
 */
public class StubMappingIndexManager {

	private final MappedIndexManager<?> indexManager;

	StubMappingIndexManager(MappedIndexManager<?> indexManager) {
		this.indexManager = indexManager;
	}

	public <T> T unwrapForTests(Class<T> clazz) {
		return clazz.cast( indexManager.toAPI() );
	}

	public IndexWorkPlan<? extends DocumentElement> createWorkPlan() {
		return createWorkPlan( new StubBackendSessionContext() );
	}

	public IndexWorkPlan<? extends DocumentElement> createWorkPlan(StubBackendSessionContext sessionContext) {
		/*
		 * Use the same defaults as in the ORM mapper for the commit strategy,
		 * but force refreshes because it's more convenient for tests.
		 */
		return indexManager.createWorkPlan( sessionContext, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE );
	}

	public IndexWorkPlan<? extends DocumentElement> createWorkPlan(StubBackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return indexManager.createWorkPlan( sessionContext, commitStrategy, refreshStrategy );
	}

	public IndexDocumentWorkExecutor<? extends DocumentElement> createDocumentWorkExecutor(
			DocumentCommitStrategy commitStrategy) {
		return createDocumentWorkExecutor( new StubBackendSessionContext(), commitStrategy );
	}

	public IndexDocumentWorkExecutor<? extends DocumentElement> createDocumentWorkExecutor(
			StubBackendSessionContext sessionContext, DocumentCommitStrategy commitStrategy) {
		return indexManager.createDocumentWorkExecutor( sessionContext, commitStrategy );
	}

	public IndexWorkExecutor createWorkExecutor() {
		return createWorkExecutor( new StubBackendSessionContext() );
	}

	public IndexWorkExecutor createWorkExecutor(StubBackendSessionContext sessionContext) {
		return createWorkExecutor( DetachedBackendSessionContext.of( sessionContext ) );
	}

	public IndexWorkExecutor createWorkExecutor(DetachedBackendSessionContext sessionContext) {
		return indexManager.createWorkExecutor( sessionContext );
	}

	/**
	 * @return A scope containing this index only.
	 */
	public StubMappingScope createScope() {
		MappedIndexScopeBuilder<DocumentReference, DocumentReference> builder =
				indexManager.createScopeBuilder( new StubBackendMappingContext() );
		return new StubMappingScope( builder.build() );
	}

	/**
	 * @return A scope containing this index and the given other indexes.
	 */
	public StubMappingScope createScope(StubMappingIndexManager... others) {
		MappedIndexScopeBuilder<DocumentReference, DocumentReference> builder =
				indexManager.createScopeBuilder( new StubBackendMappingContext() );
		for ( StubMappingIndexManager other : others ) {
			other.indexManager.addTo( builder );
		}
		return new StubMappingScope( builder.build() );
	}

	/**
	 * @return A scope containing this index and the given other indexes.
	 */
	public <R, E> GenericStubMappingScope<R, E> createGenericScope(StubMappingIndexManager... others) {
		MappedIndexScopeBuilder<R, E> builder =
				indexManager.createScopeBuilder( new StubBackendMappingContext() );
		for ( StubMappingIndexManager other : others ) {
			other.indexManager.addTo( builder );
		}
		return new GenericStubMappingScope<>( builder.build() );
	}
}

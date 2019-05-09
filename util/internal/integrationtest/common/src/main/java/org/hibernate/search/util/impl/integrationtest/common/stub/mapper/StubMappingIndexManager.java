/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.mapper;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.index.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.index.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.index.spi.IndexWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchScopeBuilder;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubMappingContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;

/**
 * A wrapper around {@link MappedIndexManager} providing some syntactic sugar,
 * such as methods that do not force to provide a session context.
 */
public class StubMappingIndexManager {

	private final MappedIndexManager<?> indexManager;

	StubMappingIndexManager(MappedIndexManager<?> indexManager) {
		this.indexManager = indexManager;
	}

	public IndexWorkPlan<? extends DocumentElement> createWorkPlan() {
		return createWorkPlan( new StubSessionContext() );
	}

	public IndexWorkPlan<? extends DocumentElement> createWorkPlan(StubSessionContext sessionContext) {
		return indexManager.createWorkPlan( sessionContext, DocumentRefreshStrategy.FORCE );
	}

	public IndexWorkPlan<? extends DocumentElement> createWorkPlan(StubSessionContext sessionContext,
			DocumentRefreshStrategy refreshStrategy) {
		return indexManager.createWorkPlan( sessionContext, refreshStrategy );
	}

	public IndexDocumentWorkExecutor<? extends DocumentElement> createDocumentWorkExecutor() {
		return createDocumentWorkExecutor( new StubSessionContext() );
	}

	public IndexDocumentWorkExecutor<? extends DocumentElement> createDocumentWorkExecutor(StubSessionContext sessionContext) {
		return indexManager.createDocumentWorkExecutor( sessionContext );
	}

	public IndexWorkExecutor createWorkExecutor() {
		return indexManager.createWorkExecutor();
	}

	/**
	 * @return A search target scoped to this index only.
	 */
	public StubMappingSearchScope createSearchScope() {
		MappedIndexSearchScopeBuilder<DocumentReference, DocumentReference> builder =
				indexManager.createSearchScopeBuilder( new StubMappingContext() );
		return new StubMappingSearchScope( builder.build() );
	}

	/**
	 * @return A search target scoped to this index and the given other indexes.
	 */
	public StubMappingSearchScope createSearchScope(StubMappingIndexManager... others) {
		MappedIndexSearchScopeBuilder<DocumentReference, DocumentReference> builder =
				indexManager.createSearchScopeBuilder( new StubMappingContext() );
		for ( StubMappingIndexManager other : others ) {
			other.indexManager.addTo( builder );
		}
		return new StubMappingSearchScope( builder.build() );
	}

	/**
	 * @return A search target scoped to this index and the given other indexes.
	 */
	public <R, E> GenericStubMappingSearchScope<R, E> createGenericSearchScope(StubMappingIndexManager... others) {
		MappedIndexSearchScopeBuilder<R, E> builder =
				indexManager.createSearchScopeBuilder( new StubMappingContext() );
		for ( StubMappingIndexManager other : others ) {
			other.indexManager.addTo( builder );
		}
		return new GenericStubMappingSearchScope<>( builder.build() );
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.mapper;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchTargetBuilder;
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
		return indexManager.createWorkPlan( sessionContext );
	}

	/**
	 * @return A search target scoped to this index only.
	 */
	public StubMappingSearchTarget createSearchTarget() {
		return new StubMappingSearchTarget( indexManager.createSearchTargetBuilder( new StubMappingContext() ).build() );
	}

	/**
	 * @return A search target scoped to this index and the given other indexes.
	 */
	public StubMappingSearchTarget createSearchTarget(StubMappingIndexManager... others) {
		MappedIndexSearchTargetBuilder builder =
				indexManager.createSearchTargetBuilder( new StubMappingContext() );
		for ( StubMappingIndexManager other : others ) {
			other.indexManager.addToSearchTarget( builder );
		}
		return new StubMappingSearchTarget( builder.build() );
	}

}

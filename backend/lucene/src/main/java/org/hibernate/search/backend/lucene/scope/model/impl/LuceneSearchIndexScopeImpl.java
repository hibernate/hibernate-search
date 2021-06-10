/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.scope.model.impl;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneMultiIndexSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneMultiIndexSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexNodeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.scope.spi.AbstractSearchIndexScope;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;

import org.apache.lucene.search.Query;

public final class LuceneSearchIndexScopeImpl
		extends AbstractSearchIndexScope<
						LuceneSearchIndexScope,
						LuceneIndexModel,
						LuceneSearchIndexNodeContext,
						LuceneSearchIndexCompositeNodeContext
				>
		implements LuceneSearchIndexScope {

	// Backend context
	private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;
	private final MultiTenancyStrategy multiTenancyStrategy;

	// Global timing source
	private final TimingSource timingSource;

	// Targeted indexes
	private final Map<String, LuceneScopeIndexManagerContext> mappedTypeNameToIndex;

	public LuceneSearchIndexScopeImpl(BackendMappingContext mappingContext,
			LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry,
			MultiTenancyStrategy multiTenancyStrategy,
			TimingSource timingSource,
			Set<? extends LuceneScopeIndexManagerContext> indexManagerContexts) {
		super( mappingContext, toModels( indexManagerContexts ) );
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.timingSource = timingSource;
		// Use LinkedHashMap/LinkedHashSet to ensure stable order when generating requests
		this.mappedTypeNameToIndex = new LinkedHashMap<>();
		for ( LuceneScopeIndexManagerContext indexManager : indexManagerContexts ) {
			this.mappedTypeNameToIndex.put( indexManager.model().mappedTypeName(), indexManager );
		}
	}

	private static Set<? extends LuceneIndexModel> toModels(
			Set<? extends LuceneScopeIndexManagerContext> indexManagerContexts) {
		return indexManagerContexts.stream().map( LuceneScopeIndexManagerContext::model )
				.collect( Collectors.toCollection( LinkedHashSet::new ) );
	}

	@Override
	protected LuceneSearchIndexScope self() {
		return this;
	}

	@Override
	public LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry() {
		return analysisDefinitionRegistry;
	}

	@Override
	public Query filterOrNull(String tenantId) {
		return multiTenancyStrategy.filterOrNull( tenantId );
	}

	@Override
	public TimeoutManager createTimeoutManager(Long timeout, TimeUnit timeUnit, boolean exceptionOnTimeout) {
		return TimeoutManager.of( timingSource, timeout, timeUnit, exceptionOnTimeout );
	}

	@Override
	public Collection<LuceneScopeIndexManagerContext> indexes() {
		return mappedTypeNameToIndex.values();
	}

	@Override
	public Map<String, ? extends LuceneSearchIndexContext> mappedTypeNameToIndex() {
		return mappedTypeNameToIndex;
	}

	@Override
	public boolean hasNestedDocuments() {
		for ( LuceneScopeIndexManagerContext element : indexes() ) {
			if ( element.model().hasNestedDocuments() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected LuceneSearchIndexCompositeNodeContext createMultiIndexSearchRootContext(
			List<LuceneSearchIndexCompositeNodeContext> rootForEachIndex) {
		return new LuceneMultiIndexSearchIndexCompositeNodeContext( this, null,
				rootForEachIndex );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected LuceneSearchIndexNodeContext createMultiIndexSearchValueFieldContext(String absolutePath,
			List<LuceneSearchIndexNodeContext> fieldForEachIndex) {
		return new LuceneMultiIndexSearchIndexValueFieldContext<>( this, absolutePath,
				(List) fieldForEachIndex );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected LuceneSearchIndexNodeContext createMultiIndexSearchObjectFieldContext(String absolutePath,
			List<LuceneSearchIndexNodeContext> fieldForEachIndex) {
		return new LuceneMultiIndexSearchIndexCompositeNodeContext( this, absolutePath,
				(List) fieldForEachIndex );
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.scope.model.impl;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.elasticsearch.common.impl.DocumentIdHelper;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchMultiIndexSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchMultiIndexSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexNodeContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.scope.spi.AbstractSearchIndexScope;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public final class ElasticsearchSearchIndexScopeImpl
		extends AbstractSearchIndexScope<
						ElasticsearchSearchIndexScope,
						ElasticsearchIndexModel,
						ElasticsearchSearchIndexNodeContext,
						ElasticsearchSearchIndexCompositeNodeContext
				>
		implements ElasticsearchSearchIndexScope {

	// Backend context
	private final Gson userFacingGson;
	private final ElasticsearchSearchSyntax searchSyntax;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final TimingSource timingSource;

	// Targeted indexes
	private final Map<String, ElasticsearchSearchIndexContext> mappedTypeNameToIndex;
	private final int maxResultWindow;

	public ElasticsearchSearchIndexScopeImpl(BackendMappingContext mappingContext,
			Gson userFacingGson, ElasticsearchSearchSyntax searchSyntax,
			MultiTenancyStrategy multiTenancyStrategy,
			TimingSource timingSource,
			Set<ElasticsearchIndexModel> indexModels) {
		super( mappingContext, indexModels );
		this.userFacingGson = userFacingGson;
		this.searchSyntax = searchSyntax;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.timingSource = timingSource;

		// Use LinkedHashMap/LinkedHashSet to ensure stable order when generating requests
		this.mappedTypeNameToIndex = new LinkedHashMap<>();
		for ( ElasticsearchIndexModel model : indexModels ) {
			mappedTypeNameToIndex.put( model.mappedTypeName(), model );
		}

		int currentMaxResultWindow = Integer.MAX_VALUE;
		for ( ElasticsearchIndexModel index : indexModels ) {
			if ( index.maxResultWindow() < currentMaxResultWindow ) {
				// take the minimum
				currentMaxResultWindow = index.maxResultWindow();
			}
		}
		this.maxResultWindow = currentMaxResultWindow;
	}

	@Override
	protected ElasticsearchSearchIndexScope self() {
		return this;
	}

	@Override
	public Gson userFacingGson() {
		return userFacingGson;
	}

	@Override
	public ElasticsearchSearchSyntax searchSyntax() {
		return searchSyntax;
	}

	@Override
	public DocumentIdHelper documentIdHelper() {
		return multiTenancyStrategy.documentIdHelper();
	}

	@Override
	public JsonObject filterOrNull(String tenantId) {
		return multiTenancyStrategy.filterOrNull( tenantId );
	}

	@Override
	public TimeoutManager createTimeoutManager(Long timeout,
			TimeUnit timeUnit, boolean exceptionOnTimeout) {
		if ( timeout != null && timeUnit != null ) {
			if ( exceptionOnTimeout ) {
				return TimeoutManager.hardTimeout( timingSource, timeout, timeUnit );
			}
			else {
				return TimeoutManager.softTimeout( timingSource, timeout, timeUnit );
			}
		}
		return TimeoutManager.noTimeout( timingSource );
	}

	@Override
	public Collection<ElasticsearchSearchIndexContext> indexes() {
		return mappedTypeNameToIndex.values();
	}

	@Override
	public Map<String, ElasticsearchSearchIndexContext> mappedTypeNameToIndex() {
		return mappedTypeNameToIndex;
	}

	@Override
	public int maxResultWindow() {
		return maxResultWindow;
	}

	@Override
	protected ElasticsearchSearchIndexCompositeNodeContext createMultiIndexSearchRootContext(
			List<ElasticsearchSearchIndexCompositeNodeContext> rootForEachIndex) {
		return new ElasticsearchMultiIndexSearchIndexCompositeNodeContext( this, null,
				rootForEachIndex );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected ElasticsearchSearchIndexNodeContext createMultiIndexSearchValueFieldContext(String absolutePath,
			List<ElasticsearchSearchIndexNodeContext> fieldForEachIndex) {
		return new ElasticsearchMultiIndexSearchIndexValueFieldContext<>( this, absolutePath,
				(List) fieldForEachIndex );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected ElasticsearchSearchIndexNodeContext createMultiIndexSearchObjectFieldContext(String absolutePath,
			List<ElasticsearchSearchIndexNodeContext> fieldForEachIndex) {
		return new ElasticsearchMultiIndexSearchIndexCompositeNodeContext( this, absolutePath,
				(List) fieldForEachIndex );
	}
}

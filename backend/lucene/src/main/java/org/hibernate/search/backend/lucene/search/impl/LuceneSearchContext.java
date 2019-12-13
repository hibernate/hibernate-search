/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.ReadIndexManagerContext;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopeModel;
import org.hibernate.search.backend.lucene.search.timeout.impl.TimeoutManager;
import org.hibernate.search.backend.lucene.search.timeout.spi.TimingSource;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentFieldValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContextImpl;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;

import org.apache.lucene.search.Query;

public final class LuceneSearchContext {

	// Mapping context
	private final ToDocumentIdentifierValueConvertContext toDocumentIdentifierValueConvertContext;
	private final ToDocumentFieldValueConvertContext toDocumentFieldValueConvertContext;

	// Backend context
	private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;
	private final MultiTenancyStrategy multiTenancyStrategy;

	// Global timing source
	private final TimingSource timingSource;

	// Targeted indexes
	private final LuceneScopeModel scopeModel;

	public LuceneSearchContext(BackendMappingContext mappingContext,
			LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry,
			MultiTenancyStrategy multiTenancyStrategy,
			TimingSource timingSource,
			LuceneScopeModel scopeModel) {
		this.toDocumentIdentifierValueConvertContext = new ToDocumentIdentifierValueConvertContextImpl( mappingContext );
		this.toDocumentFieldValueConvertContext = new ToDocumentFieldValueConvertContextImpl( mappingContext );
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.timingSource = timingSource;
		this.scopeModel = scopeModel;
	}

	public ToDocumentIdentifierValueConvertContext getToDocumentIdentifierValueConvertContext() {
		return toDocumentIdentifierValueConvertContext;
	}

	public ToDocumentFieldValueConvertContext getToDocumentFieldValueConvertContext() {
		return toDocumentFieldValueConvertContext;
	}

	public LuceneAnalysisDefinitionRegistry getAnalysisDefinitionRegistry() {
		return analysisDefinitionRegistry;
	}

	public Set<String> getIndexNames() {
		return scopeModel.getIndexNames();
	}

	public Set<? extends ReadIndexManagerContext> getIndexManagerContexts() {
		return scopeModel.getIndexManagerContexts();
	}

	public Query getFilterOrNull(String tenantId) {
		return multiTenancyStrategy.getFilterOrNull( tenantId );
	}

	public TimeoutManager createTimeoutManager(Query definitiveLuceneQuery,
			Long timeout, TimeUnit timeUnit, boolean exceptionOnTimeout) {
		if ( timeout != null && timeUnit != null ) {
			if ( exceptionOnTimeout ) {
				return TimeoutManager.hardTimeout( timingSource, definitiveLuceneQuery, timeout, timeUnit );
			}
			else {
				return TimeoutManager.softTimeout( timingSource, definitiveLuceneQuery, timeout, timeUnit );
			}
		}
		return TimeoutManager.noTimeout( timingSource, definitiveLuceneQuery );
	}

}

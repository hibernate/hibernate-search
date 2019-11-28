/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.ReadIndexManagerContext;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopeModel;
import org.hibernate.search.backend.lucene.search.timeout.impl.DefaultTimingSource;
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

	// Targeted indexes
	private final LuceneScopeModel scopeModel;

	// Global timing source
	private final TimingSource timingSource = new DefaultTimingSource();

	public LuceneSearchContext(BackendMappingContext mappingContext,
			LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry,
			MultiTenancyStrategy multiTenancyStrategy,
			LuceneScopeModel scopeModel) {
		this.toDocumentIdentifierValueConvertContext = new ToDocumentIdentifierValueConvertContextImpl( mappingContext );
		this.toDocumentFieldValueConvertContext = new ToDocumentFieldValueConvertContextImpl( mappingContext );
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
		this.multiTenancyStrategy = multiTenancyStrategy;
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

	public Query decorateLuceneQuery(Query originalLuceneQuery, String tenantId) {
		return multiTenancyStrategy.decorateLuceneQuery( originalLuceneQuery, tenantId );
	}

	public TimingSource getTimingSource() {
		return timingSource;
	}
}

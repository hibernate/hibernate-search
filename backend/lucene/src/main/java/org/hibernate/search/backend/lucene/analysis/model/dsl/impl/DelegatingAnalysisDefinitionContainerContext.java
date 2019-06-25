/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl.impl;

import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisDefinitionContainerContext;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalyzerTypeStep;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneNormalizerTypeStep;

/**
 * @deprecated This should disappear once we remove the ability to chain multiple analyzer definitions.
 */
@Deprecated
class DelegatingAnalysisDefinitionContainerContext implements LuceneAnalysisDefinitionContainerContext {

	private final LuceneAnalysisDefinitionContainerContext parentContext;

	DelegatingAnalysisDefinitionContainerContext(LuceneAnalysisDefinitionContainerContext parentContext) {
		this.parentContext = parentContext;
	}

	@Override
	public LuceneAnalyzerTypeStep analyzer(String name) {
		return parentContext.analyzer( name );
	}

	@Override
	public LuceneNormalizerTypeStep normalizer(String name) {
		return parentContext.normalizer( name );
	}

}

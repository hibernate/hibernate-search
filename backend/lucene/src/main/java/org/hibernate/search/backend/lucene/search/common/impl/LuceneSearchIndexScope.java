/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.common.impl;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.engine.search.common.spi.SearchIndexCompositeNodeContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;

public interface LuceneSearchIndexScope<S extends LuceneSearchIndexScope<?>>
		extends SearchIndexScope<S> {

	@Override
	LuceneSearchIndexNodeContext child(SearchIndexCompositeNodeContext<?> parent, String name);

	LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry();

}

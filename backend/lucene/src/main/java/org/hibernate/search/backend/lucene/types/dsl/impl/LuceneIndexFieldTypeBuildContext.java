/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.util.EventContext;

public interface LuceneIndexFieldTypeBuildContext {

	EventContext getEventContext();

	LuceneAnalysisDefinitionRegistry getAnalysisDefinitionRegistry();

}

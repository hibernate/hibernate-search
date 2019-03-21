/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import org.hibernate.search.engine.search.predicate.DslConverter;
import org.hibernate.search.util.common.reporting.EventContext;

public class LuceneIncompatibleDslConverterHandler<T> extends LuceneDslConverterHandler {

	private final String absoluteFieldPath;
	private final IndexSchemaFieldNodeComponentRetrievalStrategy<T> componentRetrievalStrategy;
	private final T component1;
	private final T component2;
	private final EventContext eventContext;

	public LuceneIncompatibleDslConverterHandler(String absoluteFieldPath, T component1, T component2, EventContext eventContext,
			IndexSchemaFieldNodeComponentRetrievalStrategy<T> componentRetrievalStrategy) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.component1 = component1;
		this.component2 = component2;
		this.eventContext = eventContext;
		this.componentRetrievalStrategy = componentRetrievalStrategy;
	}

	public void handle(DslConverter dslConverter) {
		if ( dslConverter.isEnabled() ) {
			throw componentRetrievalStrategy.createCompatibilityException( absoluteFieldPath, component1, component2, eventContext );
		}
	}
}

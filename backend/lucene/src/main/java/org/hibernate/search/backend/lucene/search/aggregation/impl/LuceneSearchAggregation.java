/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.aggregation.impl;

import java.io.IOException;
import java.util.Set;

import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorProvider;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;

public interface LuceneSearchAggregation<A> extends SearchAggregation<A>, LuceneCollectorProvider {

	/**
	 * Extract the result of the aggregation from the response.
	 *
	 * @param context The extract context, to extract information from the response's JSON body
	 * or retrieve information that was stored earlier in the {@link #request(AggregationRequestContext)}
	 * method.
	 * @return The aggregation result extracted from the response.
	 */
	A extract(AggregationExtractContext context) throws IOException;

	Set<String> getIndexNames();

}

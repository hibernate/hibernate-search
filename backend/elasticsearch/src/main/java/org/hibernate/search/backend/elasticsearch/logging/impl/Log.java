/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.backend.elasticsearch.logging.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchFieldFormatter;
import org.hibernate.search.backend.elasticsearch.index.impl.ElasticsearchIndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetBuilder;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.util.SearchException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "HSEARCH-ES")
public interface Log extends BasicLogger {

	@Message(id = 1, value = "[%1$s] Executing %2$s with parameters %3$s and body <%4$s>")
	@LogMessage(level = Level.TRACE)
	void executingWork(String host, String workType, Map<String, List<String>> parameters, String bodyAsString);

	@Message(id = 2, value = "A search query cannot target both an Elasticsearch index and other types of index."
			+ " First target was: '%1$s', other target was: '%2$s'" )
	SearchException cannotMixElasticsearchSearchTargetWithOtherType(IndexSearchTargetBuilder firstTarget, ElasticsearchIndexManager otherTarget);

	@Message(id = 3, value = "A search query cannot target multiple Elasticsearch backends."
			+ " First target was: '%1$s', other target was: '%2$s'" )
	SearchException cannotMixElasticsearchSearchTargetWithOtherBackend(IndexSearchTargetBuilder firstTarget, ElasticsearchIndexManager otherTarget);

	@Message(id = 4, value = "Unknown field '%1$s' in indexes %2$s." )
	SearchException unknownFieldForSearch(String absoluteFieldPath, Collection<String> indexNames);

	@Message(id = 5, value = "Multiple conflicting types for field '%1$s': '%2$s' in index '%3$s', but '%4$s' in index '%5$s'." )
	SearchException conflictingFieldFormattersForSearch(String absoluteFieldPath,
			ElasticsearchFieldFormatter formatter1, String indexName1,
			ElasticsearchFieldFormatter formatter2, String indexName2);

	@Message(id = 6, value = "The Elasticsearch extension can only be applied to objects"
			+ " derived from the Elasticsearch backend. Was applied to '%1$s' instead." )
	SearchException elasticsearchExtensionOnUnknownType(Object context);

	@Message(id = 7, value = "Unknown projection %1$s in indexes %2$s." )
	SearchException unknownProjectionForSearch(Collection<String> projections, Collection<String> indexNames);

	@Message(id = 8, value = "An Elasticsearch query cannot include search predicates built using a non-Elasticsearch search target."
			+ " Given predicate was: '%1$s'" )
	SearchException cannotMixElasticsearchSearchQueryWithOtherPredicates(SearchPredicate predicate);

	@Message(id = 9, value = "Field '%2$s' is not an object field in index '%1$s'." )
	SearchException nonObjectFieldForNestedQuery(String indexName, String absoluteFieldPath);

	@Message(id = 10, value = "Object field '%2$s' is not stored as nested in index '%1$s'." )
	SearchException nonNestedFieldForNestedQuery(String indexName, String absoluteFieldPath);

	@Message(id = 11, value = "An Elasticsearch query cannot include search sorts built using a non-Elasticsearch search target."
			+ " Given sort was: '%1$s'" )
	SearchException cannotMixElasticsearchSearchSortWithOtherSorts(SearchSort sort);

}

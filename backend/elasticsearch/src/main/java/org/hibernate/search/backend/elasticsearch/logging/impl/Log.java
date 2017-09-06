/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.backend.elasticsearch.logging.impl;

import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchFieldFormatter;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchTarget;
import org.hibernate.search.engine.backend.index.spi.SearchTarget;
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

	@Message(id = 2, value = "A search query cannot target both an Elasticsearch index an other types of index."
			+ " First target was: '%1$s', other target was: '%2$s'" )
	SearchException cannotMixElasticsearchSearchTargetWithOtherType(ElasticsearchSearchTarget firstTarget, SearchTarget otherTarget);

	@Message(id = 3, value = "A search query cannot target multiple Elasticsearch backends."
			+ " First target was: '%1$s', other target was: '%2$s'" )
	SearchException cannotMixElasticsearchSearchTargetWithOtherBackend(ElasticsearchSearchTarget firstTarget, ElasticsearchSearchTarget otherTarget);

	@Message(id = 4, value = "Unknown field '%1$s' in indexes %2$s." )
	SearchException unknownFieldForSearch(String absoluteFieldPath, List<String> indexNames);

	@Message(id = 5, value = "Multiple conflicting types for field '%1$s': '%2$s' in index '%3$s', but '%4$s' in index '%5$s'." )
	SearchException conflictingFieldFormattersForSearch(String absoluteFieldPath,
			ElasticsearchFieldFormatter formatter1, String indexName1,
			ElasticsearchFieldFormatter formatter2, String indexName2);

}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.logging.impl;

import java.util.List;

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.hibernate.search.analyzer.impl.AnalyzerReference;
import org.hibernate.search.elasticsearch.client.impl.BackendRequest;
import org.hibernate.search.elasticsearch.client.impl.BulkRequestFailedException;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.logging.impl.ClassFormatter;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

import com.google.gson.JsonElement;

import io.searchbox.client.JestResult;

/**
 * Hibernate Search Elasticsearch backend log abstraction.
 *
 * @author Gunnar Morling
 */
@MessageLogger(projectCode = "HSEARCH")
public interface Log extends org.hibernate.search.util.logging.impl.Log {

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 1,
			value = "Cannot execute query '%2$s', as targeted entity type '%1$s' is indexed through a non-Elasticsearch backend")
	SearchException cannotRunEsQueryTargetingEntityIndexedWithNonEsIndexManager(@FormatWith(ClassFormatter.class) Class<?> entityType, String query);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 2,
			value = "Lucene query '%1$s' cannot be transformed into equivalent Elasticsearch query" )
	SearchException cannotTransformLuceneQueryIntoEsQuery(Query query);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 3,
			value = "Lucene filter '%1$s' cannot be transformed into equivalent Elasticsearch query" )
	SearchException cannotTransformLuceneFilterIntoEsQuery(Filter filter);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 4,
			value = "The sort order RANGE_DEFINITION_ORDER should not be sent to the Elasticsearch backend" )
	SearchException cannotSendRangeDefinitionOrderToElasticsearchBackend();

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 5,
			value = "The SortType '%1$s' cannot be used with a null sort field name")
	SearchException cannotUseThisSortTypeWithNullSortFieldName(SortField.Type sortType);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 6,
			value = "Empty phrase queries are not supported")
	SearchException cannotQueryOnEmptyPhraseQuery();

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 7,
			value = "Elasticsearch request failed.\n Request:\n========\n%1$sResponse:\n=========\n%2$s"
	)
	SearchException elasticsearchRequestFailed(String request, String response, @Cause Exception cause);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 8,
			value = "Elasticsearch request failed.\n Request:\n========\n%1$sResponse:\n=========\n%2$s"
	)
	BulkRequestFailedException elasticsearchBulkRequestFailed(String request, String response, @Param List<BackendRequest<? extends JestResult>> erroneousItems);

	@LogMessage(level = Level.WARN)
	@Message(id = ES_BACKEND_MESSAGES_START_ID + 9,
			value = "Field '%2$s' in '%1$s' requires a remote analyzer reference (got '%3$s' instead). The analyzer will be ignored.")
	void analyzerIsNotRemote(@FormatWith(ClassFormatter.class) Class<?> entityType, String fieldName, AnalyzerReference analyzerReference);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 10,
			value = "Elasticsearch connection time-out; check the cluster status, it should be 'green';\n Request:\n========\n%1$sResponse:\n=========\n%2$s" )
	SearchException elasticsearchRequestTimeout(String request, String response);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 11,
			value = "Projection of non-JSON-primitive field values is not supported: '%1$s'")
	SearchException unsupportedProjectionOfNonJsonPrimitiveFields(JsonElement value);
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.elasticsearch.impl.JestAPIFormatter;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.core.BulkResult.BulkResultItem;


/**
 * @author Yoann Rodiere
 */
public class DeleteByQueryResultAssessor implements ElasticsearchRequestResultAssessor<JestResult> {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private static final int NOT_FOUND_HTTP_STATUS_CODE = 404;

	private final JestAPIFormatter formatter;

	private final DefaultElasticsearchRequestResultAssessor delegate;

	public DeleteByQueryResultAssessor(JestAPIFormatter formatter) {
		this.formatter = formatter;
		this.delegate = DefaultElasticsearchRequestResultAssessor.builder( formatter )
				.ignoreErrorStatuses( NOT_FOUND_HTTP_STATUS_CODE ).build();
	}

	@Override
	public void checkSuccess(Action<? extends JestResult> request, JestResult result) throws SearchException {
		this.delegate.checkSuccess( request, result );
		if ( result.getResponseCode() == NOT_FOUND_HTTP_STATUS_CODE ) {
			throw LOG.elasticsearchRequestDeleteByQueryNotFound( formatter.formatRequest( request ), formatter.formatResult( result ) );
		}
	}

	@Override
	public boolean isSuccess(BulkResultItem bulkResultItem) {
		throw new AssertionFailure( "This method should never be called, because DeleteByQuery actions are not Bulkable" );
	}

}

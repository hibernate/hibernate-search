/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.elasticsearch.impl.GsonService;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.ElasticsearchRequestUtils;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.core.BulkResult.BulkResultItem;


/**
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchRequestResultAssessor implements ElasticsearchRequestResultAssessor<JestResult> {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private static final int TIME_OUT_HTTP_RESPONSE_CODE = 408;

	public static final DefaultElasticsearchRequestResultAssessor INSTANCE = builder().build();

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private final Set<Integer> ignoredErrorStatuses = new HashSet<>();
		private final Set<String> ignoredErrorTypes = new HashSet<>();

		public Builder ignoreErrorStatuses(int ... ignoredErrorStatuses) {
			for ( int ignoredErrorStatus : ignoredErrorStatuses ) {
				this.ignoredErrorStatuses.add( ignoredErrorStatus );
			}
			return this;
		}

		public Builder ignoreErrorTypes(String ... ignoredErrorTypes) {
			for ( String ignoredErrorType : ignoredErrorTypes ) {
				this.ignoredErrorTypes.add( ignoredErrorType );
			}
			return this;
		}

		public DefaultElasticsearchRequestResultAssessor build() {
			return new DefaultElasticsearchRequestResultAssessor( this );
		}
	}

	private final Set<Integer> ignoredErrorStatuses;
	private final Set<String> ignoredErrorTypes;

	private DefaultElasticsearchRequestResultAssessor(Builder builder) {
		this.ignoredErrorStatuses = Collections.unmodifiableSet( new HashSet<>( builder.ignoredErrorStatuses ) );
		this.ignoredErrorTypes = Collections.unmodifiableSet( new HashSet<>( builder.ignoredErrorTypes ) );
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append( getClass().getSimpleName() ).append( "[" )
				.append( "ignoredErrorStatuses=" ).append( ignoredErrorStatuses )
				.append( ", ignoredErrorTypes=" ).append( ignoredErrorTypes )
				.append( "]" )
				.toString();
	}

	@Override
	public void checkSuccess(ElasticsearchWorkExecutionContext context, Action<? extends JestResult> request, JestResult result) throws SearchException {
		if ( !isSuccess( result ) ) {
			GsonService gsonService = context.getGsonService();
			if ( result.getResponseCode() == TIME_OUT_HTTP_RESPONSE_CODE ) {
				throw LOG.elasticsearchRequestTimeout(
						ElasticsearchRequestUtils.formatRequest( gsonService, request ),
						ElasticsearchRequestUtils.formatResponse( gsonService, result )
						);
			}
			else {
				throw LOG.elasticsearchRequestFailed(
						ElasticsearchRequestUtils.formatRequest( gsonService, request ),
						ElasticsearchRequestUtils.formatResponse( gsonService, result ),
						null );
			}
		}
	}

	@Override
	public boolean isSuccess(ElasticsearchWorkExecutionContext context, BulkResultItem resultItem) {
		// When getting a 404 for a DELETE, the error is null :(, so checking both
		return (resultItem.error == null && resultItem.status < 400 )
			|| ignoredErrorStatuses.contains( resultItem.status )
			|| ignoredErrorTypes.contains( resultItem.errorType );
	}

	private boolean isSuccess(JestResult result) {
		return result.isSucceeded()
				|| ignoredErrorStatuses.contains( result.getResponseCode() )
				|| ignoredErrorTypes.contains( getErrorType( result ) );
	}

	private String getErrorType(JestResult result) {
		JsonElement error = result.getJsonObject().get( "error" );
		if ( error == null || !error.isJsonObject() ) {
			return null;
		}

		JsonElement errorType = error.getAsJsonObject().get( "type" );
		if ( errorType == null || !errorType.isJsonPrimitive() ) {
			return null;
		}

		return errorType.getAsString();
	}

}

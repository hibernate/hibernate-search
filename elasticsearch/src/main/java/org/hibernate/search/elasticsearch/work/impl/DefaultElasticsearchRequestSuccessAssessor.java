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

import org.elasticsearch.client.Response;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.ElasticsearchClientUtils;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchRequestSuccessAssessor implements ElasticsearchRequestSuccessAssessor {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private static final JsonAccessor ROOT_ERROR_TYPE = JsonAccessor.root().property( "error" ).property( "type" );
	private static final JsonAccessor BULK_ITEM_STATUS_CODE = JsonAccessor.root().property( "status" );
	private static final JsonAccessor BULK_ITEM_ERROR_TYPE = JsonAccessor.root().property( "error" ).property( "type" );

	private static final int TIME_OUT_HTTP_STATUS_CODE = 408;

	public static final DefaultElasticsearchRequestSuccessAssessor INSTANCE = builder().build();

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

		public DefaultElasticsearchRequestSuccessAssessor build() {
			return new DefaultElasticsearchRequestSuccessAssessor( this );
		}
	}

	private final Set<Integer> ignoredErrorStatuses;
	private final Set<String> ignoredErrorTypes;

	private DefaultElasticsearchRequestSuccessAssessor(Builder builder) {
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
	public void checkSuccess(ElasticsearchWorkExecutionContext context, ElasticsearchRequest request, Response response,
			JsonObject parsedResponseBody) throws SearchException {
		if ( !isSuccess( response, parsedResponseBody ) ) {
			GsonProvider gsonProvider = context.getGsonProvider();
			if ( response.getStatusLine().getStatusCode() == TIME_OUT_HTTP_STATUS_CODE ) {
				throw LOG.elasticsearchRequestTimeout(
						ElasticsearchClientUtils.formatRequest( gsonProvider, request ),
						ElasticsearchClientUtils.formatResponse( gsonProvider, response, parsedResponseBody )
						);
			}
			else {
				throw LOG.elasticsearchRequestFailed(
						ElasticsearchClientUtils.formatRequest( gsonProvider, request ),
						ElasticsearchClientUtils.formatResponse( gsonProvider, response, parsedResponseBody ),
						null );
			}
		}
	}

	@Override
	public boolean isSuccess(ElasticsearchWorkExecutionContext context, JsonObject resultItem) {
		// Result items have the following format: { "actionName" : { "status" : 201, ... } }
		JsonObject content = resultItem.entrySet().iterator().next().getValue().getAsJsonObject();
		int statusCode = BULK_ITEM_STATUS_CODE.get( content ).getAsInt();
		return ElasticsearchClientUtils.isSuccessCode( statusCode )
			|| ignoredErrorStatuses.contains( statusCode )
			|| ignoredErrorTypes.contains( BULK_ITEM_ERROR_TYPE.get( content ).getAsString() );
	}

	private boolean isSuccess(Response response, JsonObject parsedResponseBody) {
		int code = response.getStatusLine().getStatusCode();
		return ElasticsearchClientUtils.isSuccessCode( code )
				|| ignoredErrorStatuses.contains( code )
				|| ignoredErrorTypes.contains( ROOT_ERROR_TYPE.get( parsedResponseBody ).getAsString() );
	}

}

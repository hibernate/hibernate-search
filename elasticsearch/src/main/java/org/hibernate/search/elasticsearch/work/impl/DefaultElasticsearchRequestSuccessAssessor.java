/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import static java.util.function.Predicate.isEqual;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
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

	private static final JsonAccessor<Integer> BULK_ITEM_STATUS_CODE = JsonAccessor.root().property( "status" ).asInteger();
	private static final JsonAccessor<String> ERROR_TYPE = JsonAccessor.root().property( "error" ).property( "type" ).asString();

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
	public void checkSuccess(ElasticsearchResponse response) throws SearchException {
		JsonObject responseBody = response.getBody();
		Optional<Integer> statusCode = Optional.of( response.getStatusCode() );
		checkSuccess( statusCode, responseBody );
	}

	@Override
	public void checkSuccess(JsonObject bulkResponseItem) {
		// Result items have the following format: { "actionName" : { "status" : 201, ... } }
		JsonObject responseBody = bulkResponseItem == null ? null : bulkResponseItem.entrySet().iterator().next().getValue().getAsJsonObject();
		Optional<Integer> statusCode = BULK_ITEM_STATUS_CODE.get( responseBody );
		checkSuccess( statusCode, responseBody );
	}

	private void checkSuccess(Optional<Integer> statusCode, JsonObject responseBody) {
		if ( !isSuccess( statusCode, responseBody ) ) {
			if ( statusCode.filter( isEqual( TIME_OUT_HTTP_STATUS_CODE ) ).isPresent() ) {
				throw LOG.elasticsearchRequestTimeout();
			}
			else {
				throw LOG.elasticsearchResponseIndicatesFailure();
			}
		}
	}

	private boolean isSuccess(Optional<Integer> statusCode, JsonObject responseBody) {
		return statusCode.map( (c) ->
						ElasticsearchClientUtils.isSuccessCode( c ) || ignoredErrorStatuses.contains( c )
				).orElse( false )
				|| ERROR_TYPE.get( responseBody ).map( ignoredErrorTypes::contains ).orElse( false );
	}

}

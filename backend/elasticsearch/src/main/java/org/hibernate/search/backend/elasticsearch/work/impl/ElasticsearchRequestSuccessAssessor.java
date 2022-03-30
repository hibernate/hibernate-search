/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import static java.util.function.Predicate.isEqual;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientUtils;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;



public class ElasticsearchRequestSuccessAssessor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<Integer> BULK_ITEM_STATUS_CODE = JsonAccessor.root().property( "status" ).asInteger();
	private static final JsonAccessor<String> ERROR_TYPE = JsonAccessor.root().property( "error" ).property( "type" ).asString();

	private static final int TIME_OUT_HTTP_STATUS_CODE = 408;

	public static final ElasticsearchRequestSuccessAssessor DEFAULT_INSTANCE = builder().build();

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
			Collections.addAll( this.ignoredErrorTypes, ignoredErrorTypes );
			return this;
		}

		public ElasticsearchRequestSuccessAssessor build() {
			return new ElasticsearchRequestSuccessAssessor( this );
		}
	}

	private final Set<Integer> ignoredErrorStatuses;
	private final Set<String> ignoredErrorTypes;

	private ElasticsearchRequestSuccessAssessor(Builder builder) {
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

	/**
	 * Check the given response, throwing an exception if the reponse indicates a failure.
	 * @param response The response, containing information about the outcome of the request.
	 * @throws SearchException If the result is a failure.
	 */
	public void checkSuccess(ElasticsearchResponse response) throws SearchException {
		JsonObject responseBody = response.body();
		Optional<Integer> statusCode = Optional.of( response.statusCode() );
		checkSuccess( statusCode, responseBody );
	}

	/**
	 * Check the given bulk response item, throwing an exception if it indicates a failure.
	 * @param bulkResponseItem The part of the response body concerning the request whose success is to be assessed.
	 * @throws SearchException If the result is a failure.
	 */
	public void checkSuccess(JsonObject bulkResponseItem) {
		// Result items have the following format: { "actionName" : { "status" : 201, ... } }
		JsonObject responseBody = bulkResponseItem == null ? null : bulkResponseItem.entrySet().iterator().next().getValue().getAsJsonObject();
		Optional<Integer> statusCode = BULK_ITEM_STATUS_CODE.get( responseBody );
		checkSuccess( statusCode, responseBody );
	}

	private void checkSuccess(Optional<Integer> statusCode, JsonObject responseBody) {
		if ( !isSuccess( statusCode, responseBody ) ) {
			if ( statusCode.filter( isEqual( TIME_OUT_HTTP_STATUS_CODE ) ).isPresent() ) {
				throw log.elasticsearchStatus408RequestTimeout();
			}
			else {
				throw log.elasticsearchResponseIndicatesFailure();
			}
		}
	}

	private boolean isSuccess(Optional<Integer> statusCode, JsonObject responseBody) {
		return statusCode.map(
				c -> ElasticsearchClientUtils.isSuccessCode( c ) || ignoredErrorStatuses.contains( c )
				)
				.orElse( false )
				|| ERROR_TYPE.get( responseBody ).map( ignoredErrorTypes::contains ).orElse( false );
	}

}

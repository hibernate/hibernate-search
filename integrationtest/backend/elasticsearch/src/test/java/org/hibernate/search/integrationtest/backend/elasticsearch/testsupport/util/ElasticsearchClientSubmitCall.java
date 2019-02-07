/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.util.impl.integrationtest.common.rule.Call;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.assertj.core.api.Assertions;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

class ElasticsearchClientSubmitCall extends Call<ElasticsearchClientSubmitCall> {

	private final Gson gson = new Gson();

	private final ElasticsearchRequest request;
	private final ElasticsearchRequestAssertionMode assertionMode;

	ElasticsearchClientSubmitCall(ElasticsearchRequest request) {
		this.request = request;
		this.assertionMode = null;
	}

	ElasticsearchClientSubmitCall(ElasticsearchRequest request, ElasticsearchRequestAssertionMode assertionMode) {
		this.request = request;
		this.assertionMode = assertionMode;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + request + "]";
	}

	@Override
	public boolean isSimilarTo(ElasticsearchClientSubmitCall other) {
		return request.getPath().equals( other.request.getPath() );
	}

	void verify(ElasticsearchClientSubmitCall actualCall) {
		Assertions.assertThat( actualCall.request.getPath() ).isEqualTo( request.getPath() );
		Assertions.assertThat( actualCall.request.getMethod() ).isEqualTo( request.getMethod() );
		switch ( assertionMode ) {
			case PATH_AND_METHOD:
				break;
			case EXTENSIBLE:
				Assertions.assertThat( actualCall.request.getParameters() )
						.containsAllEntriesOf( request.getParameters() );
				try {
					JSONAssert.assertEquals(
							toComparableJson( request.getBodyParts() ),
							toComparableJson( actualCall.request.getBodyParts() ),
							JSONCompareMode.STRICT_ORDER
					);
				}
				catch (JSONException e) {
					throw new IllegalStateException( "Error handling JSON for testing purposes", e );
				}
				break;
			case STRICT:
				Assertions.assertThat( actualCall.request.getParameters() ).isEqualTo( request.getParameters() );
				try {
					JSONAssert.assertEquals(
							toComparableJson( request.getBodyParts() ),
							toComparableJson( actualCall.request.getBodyParts() ),
							JSONCompareMode.STRICT
					);
				}
				catch (JSONException e) {
					throw new IllegalStateException( "Error handling JSON for testing purposes", e );
				}
				break;
		}
	}

	private String toComparableJson(List<JsonObject> bodyParts) {
		JsonArray array = new JsonArray( bodyParts.size() );
		for ( JsonObject bodyPart : bodyParts ) {
			array.add( bodyPart );
		}
		return gson.toJson( array );
	}

}

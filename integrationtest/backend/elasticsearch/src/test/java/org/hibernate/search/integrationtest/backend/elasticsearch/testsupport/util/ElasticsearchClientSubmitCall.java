/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.util.impl.integrationtest.common.rule.Call;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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
	protected String summary() {
		return getClass().getSimpleName() + "[" + request + "]";
	}

	@Override
	public boolean isSimilarTo(ElasticsearchClientSubmitCall other) {
		return request.path().equals( other.request.path() );
	}

	void verify(ElasticsearchClientSubmitCall actualCall) {
		assertSoftly( assertion -> {
			assertion.assertThat( actualCall.request.path() ).isEqualTo( request.path() );
			assertion.assertThat( actualCall.request.method() ).isEqualTo( request.method() );
			switch ( assertionMode ) {
				case PATH_AND_METHOD:
					break;
				case EXTENSIBLE:
					// containsAllEntriesOf( emptyMap ) has a special, inconsistent meaning: "the actual map is empty"
					// Avoid that...
					if ( !request.parameters().isEmpty() ) {
						assertion.assertThat( actualCall.request.parameters() )
								.containsAllEntriesOf( request.parameters() );
					}
					assertion.check( () -> assertJsonEquals(
							toComparableJson( request.bodyParts() ),
							toComparableJson( actualCall.request.bodyParts() ),
							JSONCompareMode.STRICT_ORDER
					) );
					break;
				case STRICT:
					assertion.assertThat( actualCall.request.parameters() ).isEqualTo( request.parameters() );
					assertion.check( () -> assertJsonEquals(
							toComparableJson( request.bodyParts() ),
							toComparableJson( actualCall.request.bodyParts() ),
							JSONCompareMode.STRICT
					) );
					break;
			}
		} );
	}

	private String toComparableJson(List<JsonObject> bodyParts) {
		JsonArray array = new JsonArray( bodyParts.size() );
		for ( JsonObject bodyPart : bodyParts ) {
			array.add( bodyPart );
		}
		return gson.toJson( array );
	}

}

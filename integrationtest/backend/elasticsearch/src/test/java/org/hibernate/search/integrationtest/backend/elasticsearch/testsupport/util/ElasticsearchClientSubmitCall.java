package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.util.impl.integrationtest.common.extension.Call;

import org.skyscreamer.jsonassert.JSONCompareMode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class ElasticsearchClientSubmitCall extends Call<ElasticsearchClientSubmitCall> {

	private final JsonCodecService json =
			JsonProviderHelper.create( false ).getCodec();

	private final ElasticsearchJsonRequest request;
	private final ElasticsearchRequestAssertionMode assertionMode;

	ElasticsearchClientSubmitCall(ElasticsearchJsonRequest request) {
		this.request = request;
		this.assertionMode = null;
	}

	ElasticsearchClientSubmitCall(
			org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequest wireRequest) {
		var builder = ElasticsearchJsonRequest.builder( wireRequest.method() ).wholeEncodedPath( wireRequest.path() );
		wireRequest.parameters().forEach( builder::param );
		if ( wireRequest.body() != null ) {
			try ( var reader = new BufferedReader(
					new InputStreamReader( wireRequest.body().openStream(), StandardCharsets.UTF_8 ) ) ) {
				String line;
				while ( ( line = reader.readLine() ) != null ) {
					builder.bodyJson( json.decode( line, ObjectNode.class ) );
				}
			}
			catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}
		this.request = builder.build();
		this.assertionMode = null;
	}

	ElasticsearchClientSubmitCall(ElasticsearchJsonRequest request, ElasticsearchRequestAssertionMode assertionMode) {
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
							toComparableJson( request.bodyPartsJson() ),
							toComparableJson( actualCall.request.bodyPartsJson() ),
							JSONCompareMode.STRICT_ORDER
					) );
					break;
				case STRICT:
					assertion.assertThat( actualCall.request.parameters() ).isEqualTo( request.parameters() );
					assertion.check( () -> assertJsonEquals(
							toComparableJson( request.bodyPartsJson() ),
							toComparableJson( actualCall.request.bodyPartsJson() ),
							JSONCompareMode.STRICT
					) );
					break;
			}
		} );
	}

	private String toComparableJson(List<ObjectNode> bodyParts) {
		ArrayNode array = new ArrayNode( JsonNodeFactory.instance, bodyParts.size() );
		for ( ObjectNode bodyPart : bodyParts ) {
			array.add( bodyPart );
		}
		return json.write( array );
	}

}

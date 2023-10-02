/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName;
import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.JsonObject;

import org.apache.http.HttpHost;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class ElasticsearchClientUtilsTryGetElasticsearchVersionTest {

	public static List<? extends Arguments> params() {
		return Arrays.asList(
				Arguments.of( null, "1.0.0", ElasticsearchDistributionName.ELASTIC, 1, 0, 0, null ),
				Arguments.of( null, "2.0.0", ElasticsearchDistributionName.ELASTIC, 2, 0, 0, null ),
				Arguments.of( null, "2.4.4", ElasticsearchDistributionName.ELASTIC, 2, 4, 4, null ),
				Arguments.of( null, "5.0.0", ElasticsearchDistributionName.ELASTIC, 5, 0, 0, null ),
				Arguments.of( null, "5.6.6", ElasticsearchDistributionName.ELASTIC, 5, 6, 6, null ),
				Arguments.of( null, "6.0.0", ElasticsearchDistributionName.ELASTIC, 6, 0, 0, null ),
				Arguments.of( null, "6.7.0", ElasticsearchDistributionName.ELASTIC, 6, 7, 0, null ),
				Arguments.of( null, "7.0.0-beta1", ElasticsearchDistributionName.ELASTIC, 7, 0, 0, "beta1" ),
				Arguments.of( null, "7.0.0", ElasticsearchDistributionName.ELASTIC, 7, 0, 0, null ),
				Arguments.of( "opensearch", "1.0.0-rc1", ElasticsearchDistributionName.OPENSEARCH, 1, 0, 0, "rc1" ),
				Arguments.of( "opensearch", "1.0.0", ElasticsearchDistributionName.OPENSEARCH, 1, 0, 0, null ),
				Arguments.of( "opensearch", "1.0.1", ElasticsearchDistributionName.OPENSEARCH, 1, 0, 1, null )
		);
	}

	@Mock
	private ElasticsearchClient clientMock;


	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void testValid(String distributionString, String versionString,
			ElasticsearchDistributionName expectedDistribution,
			int expectedMajor, int expectedMinor, int expectedMicro, String expectedQualifier) {
		doMock( distributionString, versionString );
		ElasticsearchVersion version = ElasticsearchClientUtils.tryGetElasticsearchVersion( clientMock );
		assertThat( version ).isNotNull();
		assertThat( version.distribution() ).isEqualTo( expectedDistribution );
		assertThat( version.majorOptional() ).hasValue( expectedMajor );
		assertThat( version.minor() ).hasValue( expectedMinor );
		assertThat( version.micro() ).hasValue( expectedMicro );
		if ( expectedQualifier != null ) {
			assertThat( version.qualifier() ).hasValue( expectedQualifier );
		}
		else {
			assertThat( version.qualifier() ).isEmpty();
		}
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void testInvalid_distribution(String distributionString, String versionString,
			ElasticsearchDistributionName expectedDistribution,
			int expectedMajor, int expectedMinor, int expectedMicro, String expectedQualifier) {
		String invalidDistributionName = "QWDWQD" + distributionString;
		doMock( invalidDistributionName, versionString );
		assertThatThrownBy( () -> ElasticsearchClientUtils.tryGetElasticsearchVersion( clientMock ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid Elasticsearch distribution name",
						"'" + invalidDistributionName.toLowerCase( Locale.ROOT ) + "'",
						"Valid names are: [elastic, opensearch]"
				);
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void testInvalid_version(String distributionString, String versionString,
			ElasticsearchDistributionName expectedDistribution,
			int expectedMajor, int expectedMinor, int expectedMicro, String expectedQualifier) {
		String invalidVersionString = versionString.substring( 0, versionString.length() - 1 ) + "-A-B";
		doMock( distributionString, invalidVersionString );
		assertThatThrownBy( () -> ElasticsearchClientUtils.tryGetElasticsearchVersion( clientMock ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid Elasticsearch version",
						"'" + invalidVersionString.toLowerCase( Locale.ROOT ) + "'",
						"Expected format is 'x.y.z-qualifier'"
				);
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void testHttpStatus404(String distributionString, String versionString,
			ElasticsearchDistributionName expectedDistribution,
			int expectedMajor, int expectedMinor, int expectedMicro, String expectedQualifier) {
		// This should only happen on Amazon OpenSearch Service,
		// which doesn't allow retrieving the version.
		when( clientMock.submit( any() ) )
				.thenReturn( CompletableFuture.completedFuture( new ElasticsearchResponse(
						new HttpHost( "mockHost:9200" ), 404, "", null ) ) );
		ElasticsearchVersion version = ElasticsearchClientUtils.tryGetElasticsearchVersion( clientMock );
		assertThat( version ).isNull();
	}

	private void doMock(String theDistributionString, String theVersionString) {
		JsonObject versionObject = new JsonObject();
		if ( theDistributionString != null ) {
			versionObject.addProperty( "distribution", theDistributionString );
		}
		versionObject.addProperty( "number", theVersionString );
		JsonObject responseBody = new JsonObject();
		responseBody.add( "version", versionObject );
		when( clientMock.submit( any() ) )
				.thenReturn( CompletableFuture.completedFuture( new ElasticsearchResponse(
						new HttpHost( "mockHost:9200" ), 200, "", responseBody ) ) );
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName;
import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.util.common.SearchException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.JsonObject;

import org.apache.http.HttpHost;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

@RunWith(Parameterized.class)
public class ElasticsearchClientUtilsTryGetElasticsearchVersionTest {

	@Parameterized.Parameters(name = "{0} - {1}")
	public static Object[][] data() {
		return new Object[][] {
				{ null, "1.0.0", ElasticsearchDistributionName.ELASTIC, 1, 0, 0, null },
				{ null, "2.0.0", ElasticsearchDistributionName.ELASTIC, 2, 0, 0, null },
				{ null, "2.4.4", ElasticsearchDistributionName.ELASTIC, 2, 4, 4, null },
				{ null, "5.0.0", ElasticsearchDistributionName.ELASTIC, 5, 0, 0, null },
				{ null, "5.6.6", ElasticsearchDistributionName.ELASTIC, 5, 6, 6, null },
				{ null, "6.0.0", ElasticsearchDistributionName.ELASTIC, 6, 0, 0, null },
				{ null, "6.7.0", ElasticsearchDistributionName.ELASTIC, 6, 7, 0, null },
				{ null, "7.0.0-beta1", ElasticsearchDistributionName.ELASTIC, 7, 0, 0, "beta1" },
				{ null, "7.0.0", ElasticsearchDistributionName.ELASTIC, 7, 0, 0, null },
				{ "opensearch", "1.0.0-rc1", ElasticsearchDistributionName.OPENSEARCH, 1, 0, 0, "rc1" },
				{ "opensearch", "1.0.0", ElasticsearchDistributionName.OPENSEARCH, 1, 0, 0, null },
				{ "opensearch", "1.0.1", ElasticsearchDistributionName.OPENSEARCH, 1, 0, 1, null }
		};
	}

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	private final String distributionString;
	private final String versionString;

	private final ElasticsearchDistributionName expectedDistribution;
	private final int expectedMajor;
	private final int expectedMinor;
	private final int expectedMicro;
	private final String expectedQualifier;

	@Mock
	private ElasticsearchClient clientMock;

	public ElasticsearchClientUtilsTryGetElasticsearchVersionTest(String distributionString, String versionString,
			ElasticsearchDistributionName expectedDistribution,
			int expectedMajor, int expectedMinor, int expectedMicro, String expectedQualifier) {
		this.distributionString = distributionString;
		this.versionString = versionString;
		this.expectedDistribution = expectedDistribution;
		this.expectedMajor = expectedMajor;
		this.expectedMinor = expectedMinor;
		this.expectedMicro = expectedMicro;
		this.expectedQualifier = expectedQualifier;
	}

	@Test
	public void testValid() {
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

	@Test
	public void testInvalid_distribution() {
		String invalidDistributionName = "QWDWQD" + distributionString;
		doMock( invalidDistributionName, versionString );
		assertThatThrownBy( () -> ElasticsearchClientUtils.tryGetElasticsearchVersion( clientMock ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid Elasticsearch distribution name",
						"'" + invalidDistributionName.toLowerCase( Locale.ROOT ) + "'",
						"Valid names are: [elastic, opensearch]" );
	}

	@Test
	public void testInvalid_version() {
		String invalidVersionString = versionString.substring( 0, versionString.length() - 1 ) + "-A-B";
		doMock( distributionString, invalidVersionString );
		assertThatThrownBy( () -> ElasticsearchClientUtils.tryGetElasticsearchVersion( clientMock ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid Elasticsearch version",
						"'" + invalidVersionString.toLowerCase( Locale.ROOT ) + "'",
						"Expected format is 'x.y.z-qualifier'" );
	}

	@Test
	public void testHttpStatus404() {
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

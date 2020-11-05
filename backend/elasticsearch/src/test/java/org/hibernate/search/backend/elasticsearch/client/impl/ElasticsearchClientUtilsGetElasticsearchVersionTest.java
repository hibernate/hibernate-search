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
import org.assertj.core.api.InstanceOfAssertFactories;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

@RunWith(Parameterized.class)
public class ElasticsearchClientUtilsGetElasticsearchVersionTest {

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] data() {
		return new Object[][] {
				{ "1.0.0", 1, 0, 0, null },
				{ "2.0.0", 2, 0, 0, null },
				{ "2.4.4", 2, 4, 4, null },
				{ "5.0.0", 5, 0, 0, null },
				{ "5.6.6", 5, 6, 6, null },
				{ "6.0.0", 6, 0, 0, null },
				{ "6.7.0", 6, 7, 0, null },
				{ "7.0.0-beta1", 7, 0, 0, "beta1" },
				{ "7.0.0", 7, 0, 0, null }
		};
	}

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	private final String versionString;
	private final int expectedMajor;
	private final int expectedMinor;
	private final int expectedMicro;
	private final String expectedQualifier;

	@Mock
	private ElasticsearchClient clientMock;

	public ElasticsearchClientUtilsGetElasticsearchVersionTest(String versionString, int expectedMajor,
			int expectedMinor, int expectedMicro, String expectedQualifier) {
		this.versionString = versionString;
		this.expectedMajor = expectedMajor;
		this.expectedMinor = expectedMinor;
		this.expectedMicro = expectedMicro;
		this.expectedQualifier = expectedQualifier;
	}

	@Test
	public void testValid() {
		doMock( versionString );
		ElasticsearchVersion version = ElasticsearchClientUtils.getElasticsearchVersion( clientMock );
		assertThat( version ).isNotNull();
		assertThat( version.major() ).isEqualTo( expectedMajor );
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
	public void testInvalid() {
		String invalidVersionString = versionString.substring( 0, versionString.length() - 1 ) + "-A-B";
		doMock( invalidVersionString );
		assertThatThrownBy( () -> ElasticsearchClientUtils.getElasticsearchVersion( clientMock ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "HSEARCH400080" )
				.extracting( Throwable::getCause, InstanceOfAssertFactories.THROWABLE )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "HSEARCH400007", invalidVersionString )
				.extracting( Throwable::getCause, InstanceOfAssertFactories.THROWABLE )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid Elasticsearch version",
						"'" + invalidVersionString.toLowerCase( Locale.ROOT ) + "'",
						"The version must be in the form 'x.y.z-qualifier'" );
	}

	private void doMock(String theVersionString) {
		JsonObject versionObject = new JsonObject();
		versionObject.addProperty( "number", theVersionString );
		JsonObject responseBody = new JsonObject();
		responseBody.add( "version", versionObject );
		when( clientMock.submit( any() ) )
				.thenReturn( CompletableFuture.completedFuture( new ElasticsearchResponse(
						new HttpHost( "mockHost:9200" ), 200, "", responseBody ) ) );
	}
}

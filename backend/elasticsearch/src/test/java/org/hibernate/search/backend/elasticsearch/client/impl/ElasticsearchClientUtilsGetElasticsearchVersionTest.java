/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.HamcrestCondition.matching;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.ExceptionMatcherBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.JsonObject;
import org.easymock.EasyMock;

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

	private final String versionString;
	private final int expectedMajor;
	private final int expectedMinor;
	private final int expectedMicro;
	private final String expectedQualifier;

	private ElasticsearchClient clientMock = EasyMock.createMock( ElasticsearchClient.class );

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
				.is( matching( ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( "HSEARCH400080" )
						.causedBy( SearchException.class )
								.withMessage( "HSEARCH400007" )
								.withMessage( invalidVersionString )
						.causedBy( SearchException.class )
								.withMessage( "Invalid Elasticsearch version" )
								.withMessage( "'" + invalidVersionString.toLowerCase( Locale.ROOT ) + "'" )
								.withMessage( "The version must be in the form 'x.y.z-qualifier'" )
						.build()
				) );
	}

	private void doMock(String theVersionString) {
		reset( clientMock );
		JsonObject versionObject = new JsonObject();
		versionObject.addProperty( "number", theVersionString );
		JsonObject responseBody = new JsonObject();
		responseBody.add( "version", versionObject );
		expect( clientMock.submit( EasyMock.anyObject() ) )
				.andReturn( CompletableFuture.completedFuture( new ElasticsearchResponse( 200, "", responseBody ) ) );
		replay( clientMock );
	}
}

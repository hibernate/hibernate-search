/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.client;

import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.Paths;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Test;

import com.google.gson.JsonObject;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

public class RequestTest {

	@Test
	public void basicRequest() {
		final JsonObject someJsonObject = JsonBuilder.object()
				.addProperty( "something", "encoded here" )
				.build();
		final ElasticsearchRequest request = ElasticsearchRequest.post()
				.pathComponent( Paths._BULK )
				.param( "refresh", false )
				.param( "quantity", 10 )
				.body( someJsonObject )
				.build();
		assertEquals( "POST", request.getMethod() );
		assertEquals( 2, request.getParameters().size() );
		assertEquals( "false", request.getParameters().get( "refresh" ) );
		assertEquals( "10", request.getParameters().get( "quantity" ) );
		assertEquals( "/" + Paths._BULK.original, request.getPath() );
		assertEquals( 1, request.getBodyParts().size() );
		assertEquals( someJsonObject, request.getBodyParts().get( 0 ) );
	}

	@Test
	@TestForIssue( jiraKey = "HSEARCH-2883" )
	public void multiPathRequest() {
		final ElasticsearchRequest request = ElasticsearchRequest.delete()
				.pathComponent( Paths._BULK )
				.pathComponent( Paths._SEARCH )
				.build();
		assertEquals( "DELETE", request.getMethod() );
		assertEquals( 0, request.getParameters().size() );
		//See also HSEARCH-2883: it is important to check that we don't add an additional '/' at the end of the generated path.
		assertEquals( "/" + Paths._BULK.original + "/" + Paths._SEARCH.original, request.getPath() );
	}

	@Test
	public void multiValuedPathRequest() {
		List<URLEncodedString> typeNames = new ArrayList<>( 2 );
		typeNames.add( URLEncodedString.fromString( "hello" ) );
		typeNames.add( URLEncodedString.fromString( "world" ) );
		final ElasticsearchRequest request = ElasticsearchRequest.post()
				.pathComponent( Paths._SEARCH )
				.multiValuedPathComponent( typeNames )
				.pathComponent( URLEncodedString.fromString( "nice day" ) )
				.build();
		assertEquals( "/_search/hello,world/nice+day", request.getPath() );
	}

}

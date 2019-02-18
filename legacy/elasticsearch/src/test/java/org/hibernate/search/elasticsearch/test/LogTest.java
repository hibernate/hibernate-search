/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import java.util.Arrays;

import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.JsonLogHelper;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.assertj.core.api.Assertions;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hibernate.search.test.util.impl.ExceptionMatcherBuilder.isException;

/**
 * @author Yoann Rodiere
 */
public class LogTest {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final Gson GSON = new Gson();

	private static final String NON_EMPTY_JSON_OBJECT_1_AS_STRING =
			"{\"property\":{\"subProperty\":\"value1\"}}";

	private static final JsonObject NON_EMPTY_JSON_OBJECT_1 =
			GSON.fromJson( NON_EMPTY_JSON_OBJECT_1_AS_STRING, JsonObject.class );

	private static final String NON_EMPTY_JSON_OBJECT_2_AS_STRING =
			"{\"property\":{\"subProperty\":\"value2\"}}";

	private static final JsonObject NON_EMPTY_JSON_OBJECT_2 =
			GSON.fromJson( NON_EMPTY_JSON_OBJECT_2_AS_STRING, JsonObject.class );

	@Test
	public void jsonObjectList_prettyPrinting() {
		JsonLogHelper logHelper = JsonLogHelper.create( new GsonBuilder(), true );

		Assertions.assertThat( logHelper.toString( Arrays.asList( NON_EMPTY_JSON_OBJECT_1, NON_EMPTY_JSON_OBJECT_2 ) ) )
				.isEqualTo(
						"\n"
						+ "{\n"
						+ "  \"property\": {\n"
						+ "    \"subProperty\": \"value1\"\n"
						+ "  }\n"
						+ "}\n"
						+ "{\n"
						+ "  \"property\": {\n"
						+ "    \"subProperty\": \"value2\"\n"
						+ "  }\n"
						+ "}\n"
				);
	}

	@Test
	public void jsonObjectList_noPrettyPrinting() {
		JsonLogHelper logHelper = JsonLogHelper.create( new GsonBuilder(), false );

		Assertions.assertThat( logHelper.toString( Arrays.asList( NON_EMPTY_JSON_OBJECT_1, NON_EMPTY_JSON_OBJECT_2 ) ) )
				.isEqualTo(
						"{\"property\":{\"subProperty\":\"value1\"}}\\n"
						+ "{\"property\":{\"subProperty\":\"value2\"}}"
				);
	}

	@Test
	public void elasticsearchRequestFailed() {
		ElasticsearchRequest request = ElasticsearchRequest.head()
				.pathComponent( URLEncodedString.fromString( "foo" ) )
				.pathComponent( URLEncodedString.fromString( "bar" ) )
				.param( "param1", "value1" )
				.param( "param2", "value2" )
				.body( NON_EMPTY_JSON_OBJECT_1 )
				.body( NON_EMPTY_JSON_OBJECT_1 )
				.build();
		ElasticsearchResponse response = new ElasticsearchResponse(
				454, "A status message", NON_EMPTY_JSON_OBJECT_2 );
		Exception cause = new Exception();

		SearchException result = log.elasticsearchRequestFailed( request, response, cause );
		Assert.assertThat(
				result,
				isException( SearchException.class )
						.withMessage( equalTo(
								"HSEARCH400007: Elasticsearch request failed.\n"
								+ "Request: HEAD /foo/bar with parameters {param1=value1, param2=value2}\n"
								+ "Response: 454 'A status message' with body \n"
								+ "{\n"
								+ "  \"property\": {\n"
								+ "    \"subProperty\": \"value2\"\n"
								+ "  }\n"
								+ "}\n"
						) )
						.causedBy( cause )
						.build()
		);
	}

	@Test
	public void elasticsearchRequestFailed_nullResponse() {
		ElasticsearchRequest request = ElasticsearchRequest.head()
				.pathComponent( URLEncodedString.fromString( "foo" ) )
				.pathComponent( URLEncodedString.fromString( "bar" ) )
				.param( "param1", "value1" )
				.param( "param2", "value2" )
				.body( NON_EMPTY_JSON_OBJECT_1 )
				.body( NON_EMPTY_JSON_OBJECT_1 )
				.build();
		Exception cause = new Exception();

		SearchException result = log.elasticsearchRequestFailed( request, null, cause );
		Assert.assertThat(
				result,
				isException( SearchException.class )
						.withMessage( equalTo(
								"HSEARCH400007: Elasticsearch request failed.\n"
								+ "Request: HEAD /foo/bar with parameters {param1=value1, param2=value2}\n"
								+ "Response: null"
						) )
						.causedBy( cause )
						.build()
		);
	}

	@Test
	public void elasticsearchBulkedRequestFailed() {
		JsonObject requestMetadata = NON_EMPTY_JSON_OBJECT_1;
		JsonObject response = NON_EMPTY_JSON_OBJECT_2;
		Exception cause = new Exception();

		SearchException result = log.elasticsearchBulkedRequestFailed( requestMetadata, response, cause );
		Assert.assertThat(
				result,
				isException( SearchException.class )
						.withMessage( equalTo(
								"HSEARCH400008: Elasticsearch bulked request failed.\n"
								+ "Request metadata: \n"
								+ "{\n"
								+ "  \"property\": {\n"
								+ "    \"subProperty\": \"value1\"\n"
								+ "  }\n"
								+ "}\n"
								+ "Response: \n"
								+ "{\n"
								+ "  \"property\": {\n"
								+ "    \"subProperty\": \"value2\"\n"
								+ "  }\n"
								+ "}\n"
						) )
						.causedBy( cause )
						.build()
		);
	}

	@Test
	public void elasticsearchBulkedRequestFailed_nullResponse() {
		JsonObject requestMetadata = NON_EMPTY_JSON_OBJECT_1;
		Exception cause = new Exception();

		SearchException result = log.elasticsearchBulkedRequestFailed( requestMetadata, null, cause );
		Assert.assertThat(
				result,
				isException( SearchException.class )
						.withMessage( equalTo(
								"HSEARCH400008: Elasticsearch bulked request failed.\n"
								+ "Request metadata: \n"
								+ "{\n"
								+ "  \"property\": {\n"
								+ "    \"subProperty\": \"value1\"\n"
								+ "  }\n"
								+ "}\n"
								+ "Response: \n"
								+ "null\n"
						) )
						.causedBy( cause )
						.build()
		);
	}

}

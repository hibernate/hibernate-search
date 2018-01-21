/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClientFactory;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClientImplementor;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.engine.impl.SimpleInitializer;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;

import org.junit.Rule;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertTrue;

/**
 * @author Yoann Rodiere
 */
@TestForIssue(jiraKey = "HSEARCH-2934")
public class ElasticsearchIndexManagerClosingIT {

	private static final String STUBBED_ELASTICSEARCH_VERSION = "5.6.0";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	@Rule
	public SearchIntegratorResource searchIntegratorResource = new SearchIntegratorResource();

	@Test
	public void closeWithPendingStreamWorks() throws Exception {
		ElasticsearchClientFactory stubClientFactory = createStrictMock( ElasticsearchClientFactory.class );
		ElasticsearchClientImplementor stubClient = createStrictMock( ElasticsearchClientImplementor.class );
		CompletableFuture<ElasticsearchResponse> blockedResponseFuture = new CompletableFuture<>();

		expect( stubClientFactory.create( anyObject() ) ).andReturn( stubClient );

		expect( stubClient.submit( requestMatching( "GET", "^$" ) ) )
				.andReturn( CompletableFuture.completedFuture( okResponse(
						"{'version': {'number':" + STUBBED_ELASTICSEARCH_VERSION + "'} }"
				) ) );

		stubClient.init( anyObject() );

		String indexUrlRegex = "^/" + Entity.INDEX_NAME + "$";
		String indexAndTypeMappingUrlRegexPrefix = "^/" + Entity.INDEX_NAME + "/"
				+ URLEncodedString.fromString( Entity.class.getName() ).encoded;

		// ----------------------------------------------
		// Expected Elasticsearch request sequence
		// 1. Init the index
		expect( stubClient.submit( requestMatching( "HEAD", indexUrlRegex ) ) )
				.andReturn( CompletableFuture.completedFuture( notFoundResponse() ) );
		expect( stubClient.submit( requestMatching( "PUT", indexUrlRegex ) ) )
				.andReturn( CompletableFuture.completedFuture( okResponse() ) );
		expect( stubClient.submit( requestMatching(
						"GET",
						"^/_cluster/health/" + Entity.INDEX_NAME + "$"
				) ) )
				.andReturn( CompletableFuture.completedFuture( okResponse() ) );
		expect( stubClient.submit( requestMatching(
						"PUT",
						indexAndTypeMappingUrlRegexPrefix + "/_mapping$"
				) ) )
				.andReturn( CompletableFuture.completedFuture( okResponse() ) );
		// 2. Index a document
		expect( stubClient.submit( requestMatching( "POST", "^/_bulk$" ) ) )
				.andReturn( blockedResponseFuture );
		// 3. And only *then* probe for index existence and delete the index
		expect( stubClient.submit( requestMatching( "HEAD", indexUrlRegex ) ) )
				.andAnswer( () -> {
					assertTrue( "Index was deleted before pending stream works were executed",
							blockedResponseFuture.isDone() );
					return CompletableFuture.completedFuture( okResponse() );
				} );
		expect( stubClient.submit( requestMatching( "DELETE", indexUrlRegex ) ) )
				.andReturn( CompletableFuture.completedFuture( okResponse() ) );
		// End of expected Elasticsearch request sequence
		// ----------------------------------------------

		stubClient.close();

		replay( stubClientFactory, stubClient );

		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addClass( Entity.class )
				.addProvidedService( ElasticsearchClientFactory.class, stubClientFactory )
				.addProperty(
						// This is absolutely mandatory to reproduce the bug
						"hibernate.search.default." + ElasticsearchEnvironment.INDEX_SCHEMA_MANAGEMENT_STRATEGY,
						IndexSchemaManagementStrategy.DROP_AND_CREATE_AND_DROP.getExternalName()
				);


		ExtendedSearchIntegrator searchIntegrator = searchIntegratorResource.create( configuration );

		Entity entity = new Entity( 0 );
		AddLuceneWork work = searchIntegrator.getIndexBinding( Entity.TYPE_IDENTIFIER )
				.getDocumentBuilder().createAddWork(
						null, Entity.TYPE_IDENTIFIER, entity, 0, "0",
						SimpleInitializer.INSTANCE, new ContextualExceptionBridgeHelper()
				);
		// Important: we actually want to perform a stream, async operation here.
		searchIntegrator.getIndexManager( Entity.INDEX_NAME )
				.performStreamOperation( work, null, true );

		/*
		 * Schedule the indexing work to be actually executed a long time from now.
		 *
		 * This simulates the condition in which the bug used to occur:
		 * the stream work queue is filled with plenty of works,
		 * those works are still being processed,
		 * and yet the index manager is asked to close.
		 * We need to make sure that the index manager will wait for
		 * the works to be processed before destroying anything,
		 * in particular before dropping the index.
		 *
		 * In this test, we assert that the index manager actually waited
		 * in the stub answer to the HEAD call (see above).
		 */
		ForkJoinPool.commonPool().submit( () -> {
			try {
				Thread.sleep( 1_000 );
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			blockedResponseFuture.complete( okResponse( "{'items': [{'index': {'created': true, 'status': 201}}]}" ) );
		} );

		// Should not trigger an exception
		searchIntegrator.close();
	}

	private ElasticsearchRequest requestMatching(String method, String regex) {
		Pattern pattern = Pattern.compile( regex, Pattern.CASE_INSENSITIVE );
		EasyMock.reportMatcher( new IArgumentMatcher() {
			@Override
			public boolean matches(Object argument) {
				if ( !( argument instanceof ElasticsearchRequest ) ) {
					return false;
				}
				ElasticsearchRequest request = (ElasticsearchRequest) argument;
				return Objects.equals( method, request.getMethod() )
						&& pattern.matcher( request.getPath() ).matches();
			}

			@Override
			public void appendTo(StringBuffer buffer) {
				buffer.append( "requestMatching(" )
						.append( method )
						.append( "," )
						.append( regex )
						.append( ")" );
			}
		} );
		return null;
	}

	private static ElasticsearchResponse notFoundResponse() {
		return new ElasticsearchResponse( 404, "Not found", null );
	}

	private static ElasticsearchResponse okResponse() {
		return okResponse( null );
	}

	private static ElasticsearchResponse okResponse(String body) {
		JsonObject jsonObjectBody = null;
		if ( body != null ) {
			jsonObjectBody = GSON.fromJson( body, JsonObject.class );
		}
		return new ElasticsearchResponse( 200, "OK", jsonObjectBody );
	}

	@Indexed(index = Entity.INDEX_NAME)
	private static class Entity {

		public static final String INDEX_NAME = "index_name";

		private static final IndexedTypeIdentifier TYPE_IDENTIFIER = PojoIndexedTypeIdentifier.convertFromLegacy( Entity.class );

		@DocumentId
		private Integer id;

		public Entity(Integer id) {
			this.id = id;
		}
	}
}

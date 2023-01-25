/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.search.query;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultReadAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.encodeName;
import static org.junit.Assume.assumeTrue;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.impl.ElasticsearchBackendImplSettings;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.StubSingleIndexLayoutStrategy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Test the content of generated Elasticsearch search queries.
 */
@RunWith(Parameterized.class)
public class ElasticsearchSearchQueryIT {

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Parameterized.Parameters(name = "IndexLayoutStrategy = {0}")
	public static Object[][] configurations() {
		return new Object[][] {
				{ null, defaultReadAlias( index.name() ) },
				{ "no-alias", encodeName( index.name() ) },
				{ new StubSingleIndexLayoutStrategy( "custom-write", "custom-read" ), encodeName( "custom-read" ) }
		};
	}

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ElasticsearchClientSpy clientSpy = new ElasticsearchClientSpy();

	private final Object layoutStrategy;
	private final URLEncodedString readName;

	public ElasticsearchSearchQueryIT(Object layoutStrategy, URLEncodedString readName) {
		this.layoutStrategy = layoutStrategy;
		this.readName = readName;
	}

	@Before
	public void setup() {
		setupHelper.start()
				.withBackendProperty(
						ElasticsearchBackendImplSettings.CLIENT_FACTORY, clientSpy.factoryReference()
				)
				.withBackendProperty(
						ElasticsearchBackendSettings.LAYOUT_STRATEGY, layoutStrategy
				)
				.withIndex( index )
				.setup();
	}

	@Test
	public void simple() {
		StubMappingScope scope = index.createScope();

		SearchQuery<?> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();

		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( readName )
						.pathComponent( Paths._SEARCH )
						.body( new JsonObject() ) // We don't care about the payload
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		query.fetchAll();
	}

	@Test
	public void defaultSourceFiltering() {
		StubMappingScope scope = index.createScope();

		SearchQuery<?> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();

		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( readName )
						.pathComponent( Paths._SEARCH )
						.body( new Gson().fromJson( "{'_source':false}", JsonObject.class ) )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		query.fetchAll();
	}

	@Test
	public void projection_sourceFiltering() {
		StubMappingScope scope = index.createScope();

		SearchQuery<Object> query = scope.query()
				.select( f -> f.field( "string" ) )
				.where( f -> f.matchAll() )
				.toQuery();

		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( readName )
						.pathComponent( Paths._SEARCH )
						.body( new Gson().fromJson( "{'_source':['string']}", JsonObject.class ) )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		query.fetchAll();
	}

	@Test
	public void routing() {
		StubMappingScope scope = index.createScope();

		String routingKey = "someRoutingKey";

		SearchQuery<?> query = scope.query()
				.where( f -> f.matchAll() )
				.routing( routingKey )
				.toQuery();

		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( readName )
						.pathComponent( Paths._SEARCH )
						.body( new JsonObject() ) // We don't care about the payload
						.param( "routing", routingKey )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		query.fetchAll();
	}

	@Test
	public void trackTotalHits_fetch() {
		assumeTrue(
				"Run only if the Elasticsearch version supports `track_total_hits` parameter",
				TckConfiguration.get().getBackendFeatures().supportsTotalHitsThresholdForSearch()
		);

		StubMappingScope scope = index.createScope();

		SearchQuery<?> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();

		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.param( "track_total_hits", true )
						.pathComponent( readName )
						.pathComponent( Paths._SEARCH )
						.body( new JsonObject() ) // We don't care about the payload
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		query.fetch( 30 );
	}

	@Test
	public void trackTotalHits_fetchHits() {
		assumeTrue(
				"Run only if the Elasticsearch version supports `track_total_hits` parameter",
				TckConfiguration.get().getBackendFeatures().supportsTotalHitsThresholdForSearch()
		);

		StubMappingScope scope = index.createScope();

		SearchQuery<?> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();

		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.param( "track_total_hits", false )
						.pathComponent( readName )
						.pathComponent( Paths._SEARCH )
						.body( new JsonObject() ) // We don't care about the payload
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		query.fetchHits( 30 );
	}

	@SuppressWarnings("unused")
	private static class IndexBinding {
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			integer = root.field(
					"integer",
					f -> f.asInteger().projectable( Projectable.YES )
			)
					.toReference();
			string = root.field(
					"string",
					f -> f.asString().projectable( Projectable.YES )
			)
					.toReference();
		}
	}


}

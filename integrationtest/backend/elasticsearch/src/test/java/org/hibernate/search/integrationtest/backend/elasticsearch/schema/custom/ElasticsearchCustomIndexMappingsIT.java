/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.custom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultAliases;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultPrimaryName;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.cfg.spi.ElasticsearchBackendSpiSettings;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Rule;
import org.junit.Test;

import com.google.gson.JsonObject;

public class ElasticsearchCustomIndexMappingsIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ElasticsearchClientSpy clientSpy = new ElasticsearchClientSpy();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private final ElasticsearchTestDialect dialect = ElasticsearchTestDialect.get();

	@Test
	public void noConflicts() {
		verify( "no-conflicts.json", mappings -> {
			JsonObject source = new JsonObject();
			source.addProperty( "enabled", false );
			mappings.add( "_source", source );

			JsonObject properties = new JsonObject();
			JsonObject integer = new JsonObject();
			properties.add( "integer", integer );
			integer.addProperty( "type", "integer" );
			integer.addProperty( "index", true );
			integer.addProperty( "doc_values", false );
			mappings.add( "properties", properties );
		} );
	}

	public void verify(String mappingsFile, Consumer<JsonObject> expectedMappings) {
		clientSpy.expectNext(
				ElasticsearchRequest.get().build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		clientSpy.expectNext(
				ElasticsearchRequest.get()
						.multiValuedPathComponent( defaultAliases( index.name() ) )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		clientSpy.expectNext(
				ElasticsearchRequest.put()
						.pathComponent( defaultPrimaryName( index.name() ) )
						.body( createIndex( expectedMappings ) )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		setupHelper.start()
				.withBackendProperty(
						ElasticsearchBackendSpiSettings.CLIENT_FACTORY,
						clientSpy.factoryReference()
				)
				.withIndex( index )
				.withIndexProperty(
						index.name(), ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MAPPINGS_FILE,
						"custom-index-mappings/" + mappingsFile
				)
				.setup();

		clientSpy.verifyExpectationsMet();

		initData( 12 );

		List<DocumentReference> hits = index.createScope().query()
				.where( f -> f.match().field( "string" ).matching( "value3" ) )
				.fetchHits( 12 );

		assertThat( hits ).hasSize( 3 );
	}

	private JsonObject createIndex(Consumer<JsonObject> expectedMappings) {
		JsonObject payload = new JsonObject();

		JsonObject mappings = new JsonObject();
		payload.add( "mappings", mappings );

		JsonObject mapping = dialect.getTypeNameForMappingAndBulkApi()
				// ES6 and below: the mapping has its own object node, child of "mappings"
				.map( name -> {
					JsonObject doc = new JsonObject();
					mappings.add( name.original, doc );
					return doc;
				} )
				// ES7 and below: the mapping is the "mappings" node
				.orElse( mappings );

		expectedMappings.accept( mapping );
		return payload;
	}

	private void initData(int documentCount) {
		index.bulkIndexer()
				.add( documentCount, i -> documentProvider(
						String.valueOf( i ),
						document -> document.addValue( index.binding().string, "value" + ( i % 4 ) )
				) )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() ).toReference();
		}
	}
}

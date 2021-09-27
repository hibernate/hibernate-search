/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.custom;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultAliases;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultPrimaryName;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.cfg.spi.ElasticsearchBackendSpiSettings;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Rule;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ElasticsearchCustomIndexComplexMappingsIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ElasticsearchClientSpy clientSpy = new ElasticsearchClientSpy();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private final ElasticsearchTestDialect dialect = ElasticsearchTestDialect.get();

	@Test
	public void test() throws Exception {
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
						.body( expectedPayload() )
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
						"custom-index-mappings/complex-conflicts.json"
				)
				.setup();

		clientSpy.verifyExpectationsMet();
	}

	private JsonObject expectedPayload() throws Exception {
		JsonElement mappings = expectedMappings();

		JsonElement mapping = dialect.getTypeNameForMappingAndBulkApi()
				// ES6 and below: the mapping has its own object node, child of "mappings"
				.map( name -> {
					JsonObject doc = new JsonObject();
					doc.add( name.original, mappings );
					return (JsonElement) doc;
				} )
				// ES7 and below: the mapping is the "mappings" node
				.orElse( mappings );

		JsonObject payload = new JsonObject();
		payload.add( "mappings", mapping );

		return payload;
	}

	private JsonElement expectedMappings() throws Exception {
		try ( InputStream in = ElasticsearchCustomIndexComplexMappingsIT.class.getClassLoader()
				.getResourceAsStream( "custom-index-mappings/complex-conflicts-merged.json" ) ) {

			Reader reader = new InputStreamReader( in, "UTF-8" );
			return new JsonParser().parse( reader );
		}
	}

	private static class IndexBinding {
		final IndexFieldReference<String> searchField;
		final IndexFieldReference<String> bothField;

		final IndexObjectFieldReference searchObject;

		final IndexObjectFieldReference bothObject;
		final IndexFieldReference<String> searchNested;
		final IndexFieldReference<String> bothNested;

		final IndexObjectFieldReference searchNestedObject;

		final IndexObjectFieldReference bothNestedObject;
		final IndexFieldReference<String> searchNestedNested;
		final IndexFieldReference<String> bothNestedNested;

		IndexBinding(IndexSchemaElement root) {
			searchField = root.field( "searchField", f -> f.asString() ).toReference();
			bothField = root.field( "bothField", f -> f.asString() ).toReference();

			searchObject = root.objectField( "searchObject" ).toReference();

			IndexSchemaObjectField bObject = root.objectField( "bothObject" );
			bothObject = bObject.toReference();
			searchNested = bObject.field( "searchNested", f -> f.asString() ).toReference();
			bothNested = bObject.field( "bothNested", f -> f.asString() ).toReference();

			searchNestedObject = bObject.objectField( "searchNestedObject" ).toReference();

			IndexSchemaObjectField bNestedObject = bObject.objectField( "bothNestedObject" );
			bothNestedObject = bNestedObject.toReference();
			searchNestedNested = bNestedObject.field( "searchNestedNested", f -> f.asString() ).toReference();
			bothNestedNested = bNestedObject.field( "bothNestedNested", f -> f.asString() ).toReference();
		}
	}
}

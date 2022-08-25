/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.mapping;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultAliases;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultPrimaryName;

import java.util.function.Consumer;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.cfg.spi.ElasticsearchBackendImplSettings;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.Rule;
import org.junit.Test;

import com.google.gson.JsonObject;

public class ElasticsearchFieldAttributesIT {

	private final ElasticsearchTestDialect dialect = ElasticsearchTestDialect.get();

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ElasticsearchClientSpy clientSpy = new ElasticsearchClientSpy();

	@Test
	public void verifyNorms() {
		JsonObject properties = new JsonObject();
		properties.add( "keyword", fieldWithNorms( "keyword", false ) );
		properties.add( "text", fieldWithNorms( "text", true ) );
		properties.add( "norms", fieldWithNorms( "keyword", true ) );
		properties.add( "omitNorms", fieldWithNorms( "text", false ) );

		matchMapping( root -> {
			root.field( "keyword", f -> f.asString() ).toReference();
			root.field( "text", f -> f.asString().analyzer( "standard" ) ).toReference();
			root.field( "norms", f -> f.asString().norms( Norms.YES ) ).toReference();
			root.field( "omitNorms", f -> f.asString().analyzer( "standard" ).norms( Norms.NO ) ).toReference();
		}, properties );
	}

	@Test
	public void verifyTermVector() {
		JsonObject properties = new JsonObject();
		properties.add( "no", fieldWithTermVector( "no" ) );
		properties.add( "yes", fieldWithTermVector( "yes" ) );
		properties.add( "implicit", fieldWithTermVector( "no" ) );
		properties.add( "moreOptions", fieldWithTermVector( "with_positions_offsets_payloads" ) );

		matchMapping( root -> {
			root.field( "no", f -> f.asString().analyzer( "standard" ).termVector( TermVector.NO ) ).toReference();
			root.field( "yes", f -> f.asString().analyzer( "standard" ).termVector( TermVector.YES ) ).toReference();
			root.field( "implicit", f -> f.asString().analyzer( "standard" ) ).toReference();
			root.field( "moreOptions", f -> f.asString().analyzer( "standard" ).termVector( TermVector.WITH_POSITIONS_OFFSETS_PAYLOADS ) ).toReference();
		}, properties );
	}

	@Test
	public void verifyNative() {
		JsonObject nativeField = new JsonObject();
		nativeField.addProperty( "type", "half_float" );
		nativeField.addProperty( "ignore_malformed", true );

		JsonObject properties = new JsonObject();
		properties.add( "native", nativeField );

		matchMapping( root -> {
			root.field(
					"native",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative().mapping( "{\"type\": \"half_float\",\"ignore_malformed\": true}" )
			)
					.toReference();
		}, properties );
	}

	private void matchMapping(Consumer<IndexSchemaElement> mapping, JsonObject properties) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( mapping );
		clientSpy.expectNext(
				ElasticsearchRequest.get().build(),
				ElasticsearchRequestAssertionMode.STRICT
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
						.body( createIndex( properties ) )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		setupHelper.start()
				.withBackendProperty( ElasticsearchBackendImplSettings.CLIENT_FACTORY, clientSpy.factoryReference() )
				.withIndex( index )
				.setup();

		clientSpy.verifyExpectationsMet();
	}

	private JsonObject createIndex(JsonObject properties) {
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

		mapping.add( "properties", properties );
		return payload;
	}

	private static JsonObject fieldWithNorms(String type, boolean value) {
		JsonObject field = new JsonObject();
		field.addProperty( "type", type );
		field.addProperty( "norms", value );
		return field;
	}

	private static JsonObject fieldWithTermVector(String termVector) {
		JsonObject field = new JsonObject();
		field.addProperty( "type", "text" );
		field.addProperty( "term_vector", termVector );
		return field;
	}
}

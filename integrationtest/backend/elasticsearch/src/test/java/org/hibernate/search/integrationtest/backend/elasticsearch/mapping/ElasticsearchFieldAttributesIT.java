/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.mapping;

import org.hibernate.search.backend.elasticsearch.cfg.spi.ElasticsearchBackendSpiSettings;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.dialect.ElasticsearchTestDialect;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;

import org.junit.Rule;
import org.junit.Test;

import com.google.gson.JsonObject;

public class ElasticsearchFieldAttributesIT {

	private static final String BACKEND_NAME = "my-backend";
	private static final String INDEX_NAME = "my-index";

	private final ElasticsearchTestDialect dialect = ElasticsearchTestDialect.get();

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ElasticsearchClientSpy clientSpy = new ElasticsearchClientSpy();

	@Test
	public void spyMappedAttributes() {
		clientSpy.expectNext(
				ElasticsearchRequest.get().build(), ElasticsearchRequestAssertionMode.STRICT
		);

		clientSpy.expectNext(
				ElasticsearchRequest.head().pathComponent( URLEncodedString.fromString( INDEX_NAME ) ).build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		clientSpy.expectNext(
				ElasticsearchRequest.put()
						.pathComponent( URLEncodedString.fromString( INDEX_NAME ) )
						.body( indexCreationPayload() )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		setupHelper.withDefaultConfiguration( BACKEND_NAME )
				.withBackendProperty(
						BACKEND_NAME, ElasticsearchBackendSpiSettings.CLIENT_FACTORY, clientSpy.getFactory()
				)
				.withIndex(
						INDEX_NAME,
						ctx -> new IndexMapping( ctx.getSchemaElement() )
				)
				.setup();

		clientSpy.verifyExpectationsMet();
	}

	private JsonObject indexCreationPayload() {
		JsonObject payload = new JsonObject();

		JsonObject mappings = new JsonObject();
		payload.add( "mappings", mappings );

		JsonObject mapping = dialect.getTypeNameForMappingApi()
				// ES6 and below: the mapping has its own object node, child of "mappings"
				.map( name -> {
					JsonObject doc = new JsonObject();
					mappings.add( name.original, doc );
					return doc;
				} )
				// ES7 and below: the mapping is the "mappings" node
				.orElse( mappings );

		JsonObject properties = new JsonObject();
		mapping.add( "properties", properties );

		properties.add( "keyword", json( "keyword", false ) );
		properties.add( "text", json( "text", true ) );
		properties.add( "norms", json( "keyword", true ) );
		properties.add( "omitNorms", json( "text", false ) );
		return payload;
	}

	private static JsonObject json(String type, boolean norms) {
		JsonObject field = new JsonObject();
		field.addProperty( "type", type );
		field.addProperty( "norms", norms );
		return field;
	}

	private static class IndexMapping {
		IndexMapping(IndexSchemaElement root) {
			root.field( "keyword", f -> f.asString() ).toReference();
			root.field( "text", f -> f.asString().analyzer( "standard" ) ).toReference();
			root.field( "norms", f -> f.asString().norms( Norms.YES ) ).toReference();
			root.field( "omitNorms", f -> f.asString().analyzer( "standard" ).norms( Norms.NO ) ).toReference();
		}
	}
}

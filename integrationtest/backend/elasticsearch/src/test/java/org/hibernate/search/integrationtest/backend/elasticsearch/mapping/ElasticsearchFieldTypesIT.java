/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.mapping;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultAliases;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultPrimaryName;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.cfg.impl.ElasticsearchBackendImplSettings;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchTckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.Rule;
import org.junit.Test;

import com.google.gson.JsonObject;

/**
 * Test the property types defined on Elasticsearch server
 */
public class ElasticsearchFieldTypesIT {

	private final ElasticsearchTestDialect dialect = ElasticsearchTestDialect.get();

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public final ElasticsearchClientSpy clientSpy = new ElasticsearchClientSpy();

	private final StubMappedIndex index = StubMappedIndex.ofNonRetrievable( IndexBinding::new );

	@Test
	public void test() {
		if ( ElasticsearchTckBackendFeatures.supportsVersionCheck() ) {
			clientSpy.expectNext(
					ElasticsearchRequest.get().build(),
					ElasticsearchRequestAssertionMode.STRICT
			);
		}
		clientSpy.expectNext(
				ElasticsearchRequest.get()
						.multiValuedPathComponent( defaultAliases( index.name() ) )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		clientSpy.expectNext(
				ElasticsearchRequest.put()
						.pathComponent( defaultPrimaryName( index.name() ) )
						.body( indexCreationPayload() )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		setupHelper.start()
				.withBackendProperty(
						ElasticsearchBackendImplSettings.CLIENT_FACTORY, clientSpy.factoryReference()
				)
				.withIndex( index )
				.setup();

		clientSpy.verifyExpectationsMet();
	}

	private JsonObject indexCreationPayload() {
		JsonObject payload = new JsonObject();

		JsonObject mappings = new JsonObject();
		payload.add( "mappings", mappings );

		JsonObject properties = new JsonObject();
		mappings.add( "properties", properties );

		properties.add( "keyword", type( "keyword" ) );
		properties.add( "text", type( "text" ) );
		properties.add( "integer", type( "integer" ) );
		properties.add( "long", type( "long" ) );
		properties.add( "boolean", type( "boolean" ) );
		properties.add( "byte", type( "byte" ) );
		properties.add( "short", type( "short" ) );
		properties.add( "float", type( "float" ) );
		properties.add( "double", type( "double" ) );
		properties.add( "unsupportedType", type( "half_float" ) );

		return payload;
	}

	private static JsonObject type(String type) {
		JsonObject field = new JsonObject();
		field.addProperty( "type", type );
		return field;
	}

	private static class IndexBinding {
		IndexBinding(IndexSchemaElement root) {
			// string type + not analyzed => keyword
			root.field( "keyword", f -> f.asString() ).toReference();

			// string type + analyzed => text
			root.field( "text", f -> f.asString().analyzer( "standard" ) ).toReference();

			root.field( "integer", f -> f.asInteger() ).toReference();
			root.field( "long", f -> f.asLong() ).toReference();
			root.field( "boolean", f -> f.asBoolean() ).toReference();
			root.field( "byte", f -> f.asByte() ).toReference();
			root.field( "short", f -> f.asShort() ).toReference();
			root.field( "float", f -> f.asFloat() ).toReference();
			root.field( "double", f -> f.asDouble() ).toReference();
			root.field(
					"unsupportedType",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative().mapping( "{\"type\": \"half_float\"}" )
			)
					.toReference();
		}
	}
}

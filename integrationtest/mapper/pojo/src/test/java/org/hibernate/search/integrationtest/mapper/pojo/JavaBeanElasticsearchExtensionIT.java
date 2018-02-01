/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.IndexSchemaElement;
import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.engine.common.SearchMappingRepository;
import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.JavaBeanMappingContributor;
import org.hibernate.search.engine.mapper.model.SearchModel;
import org.hibernate.search.mapper.pojo.bridge.Bridge;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingDefinition;
import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.PojoModelElement;
import org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.integrationtest.util.common.StubClientElasticsearchBackendFactory;
import org.hibernate.search.integrationtest.util.common.StubElasticsearchClient;
import org.hibernate.search.integrationtest.util.common.StubElasticsearchClient.Request;
import org.hibernate.search.engine.search.SearchQuery;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.json.JSONException;

import static org.hibernate.search.integrationtest.util.common.StubAssert.assertDropAndCreateIndexRequests;
import static org.hibernate.search.integrationtest.util.common.StubAssert.assertIndexDocumentRequest;
import static org.hibernate.search.integrationtest.util.common.StubAssert.assertRequest;

/**
 * @author Yoann Rodiere
 */
public class JavaBeanElasticsearchExtensionIT {

	private SearchMappingRepository mappingRepository;

	private JavaBeanMapping mapping;

	private static final String HOST_1 = "http://es1.mycompany.com:9200/";

	@Before
	public void setup() throws JSONException {
		SearchMappingRepositoryBuilder mappingRepositoryBuilder = SearchMappingRepository.builder()
				.setProperty( "backend.elasticsearchBackend_1.type", StubClientElasticsearchBackendFactory.class.getName() )
				.setProperty( "backend.elasticsearchBackend_1.host", HOST_1 )
				.setProperty( "index.default.backend", "elasticsearchBackend_1" );

		JavaBeanMappingContributor contributor = new JavaBeanMappingContributor( mappingRepositoryBuilder );

		ProgrammaticMappingDefinition mappingDefinition = contributor.programmaticMapping();
		mappingDefinition.type( IndexedEntity.class )
				.indexed( IndexedEntity.INDEX )
				.property( "id" )
						.documentId()
				.property( "jsonString" )
						.bridge( MyElasticsearchBridgeImpl.class );

		mappingRepository = mappingRepositoryBuilder.build();
		mapping = contributor.getResult();

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();

		assertDropAndCreateIndexRequests( requests, IndexedEntity.INDEX, HOST_1,
				"{"
					+ "'properties': {"
						+ "'jsonStringField': {"
							// As defined in MyElasticsearchBridgeImpl
							+ "'esAttribute1': 'val1'"
						+ "}"
					+ "}"
				+ "}" );
	}

	@After
	public void cleanup() {
		StubElasticsearchClient.drainRequestsByIndex();
		if ( mappingRepository != null ) {
			mappingRepository.close();
		}
	}

	@Test
	public void index() throws JSONException {
		try ( PojoSearchManager manager = mapping.createSearchManager() ) {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setJsonString( "{'esProperty1':'val1'}" );

			manager.getMainWorker().add( entity1 );
		}

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();
		assertIndexDocumentRequest( requests, IndexedEntity.INDEX, 0, HOST_1, "1",
				"{"
					+ "'jsonStringField': {"
						+ "'esProperty1': 'val1'"
					+ "}"
				+ "}" );
	}

	@Test
	public void search() throws JSONException {
		try ( PojoSearchManager manager = mapping.createSearchManager() ) {
			SearchQuery<PojoReference> query = manager.search( IndexedEntity.class )
					.query()
					.asReferences()
					.predicate().bool( b -> {
						b.should().withExtension( ElasticsearchExtension.get() )
								.fromJsonString( "{'es1': 'val1'}" );
						b.should().withExtensionOptional(
								ElasticsearchExtension.get(),
								// FIXME find some way to forbid using the context twice... ?
								c -> c.fromJsonString( "{'es2': 'val2'}" )
						);
						b.must().withExtensionOptional(
								ElasticsearchExtension.get(),
								// FIXME find some way to forbid using the context twice... ?
								c -> c.fromJsonString( "{'es3': 'val3'}" ),
								c -> c.match().onField( "fallback1" ).matching( "val1" )
						);
					} )
					.build();

			query.execute();
		}

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();
		assertRequest( requests, Arrays.asList( IndexedEntity.INDEX ), 0,
				HOST_1, "POST", "/_search",
				null,
				"{"
					+ "'query': {"
						+ "'bool': {"
							+ "'should': ["
								+ "{"
									+ "'es1': 'val1'"
								+ "},"
								+ "{"
									+ "'es2': 'val2'"
								+ "}"
							+ "],"
							+ "'must': {"
								+ "'es3': 'val3'"
							+ "}"
						+ "}"
					+ "}"
				+ "}" );
	}

	public static final class IndexedEntity {

		public static final String INDEX = "IndexedEntity";

		private Integer id;

		private String jsonString;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getJsonString() {
			return jsonString;
		}

		public void setJsonString(String jsonString) {
			this.jsonString = jsonString;
		}

	}

	public static final class MyElasticsearchBridgeImpl implements Bridge {

		private PojoModelElementAccessor<String> sourceAccessor;
		private IndexFieldAccessor<String> fieldAccessor;

		@Override
		public void bind(IndexSchemaElement indexSchemaElement, PojoModelElement bridgedPojoModelElement,
				SearchModel searchModel) {
			sourceAccessor = bridgedPojoModelElement.createAccessor( String.class );
			fieldAccessor = indexSchemaElement.field( "jsonStringField" )
					.withExtension( ElasticsearchExtension.get() )
					.asJsonString(
							"{"
								+ "'esAttribute1': 'val1'"
							+ "}"
					).createAccessor();
		}

		@Override
		public void write(DocumentElement target, PojoElement source) {
			String sourceValue = sourceAccessor.read( source );
			fieldAccessor.write( target, sourceValue );
		}

	}
}

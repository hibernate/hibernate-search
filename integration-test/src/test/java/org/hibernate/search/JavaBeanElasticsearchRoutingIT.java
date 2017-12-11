/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search;

import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.client.impl.StubElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.impl.StubElasticsearchClient.Request;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchBackendFactory;
import org.hibernate.search.engine.common.SearchMappingRepository;
import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.JavaBeanMappingContributor;
import org.hibernate.search.mapper.pojo.model.spi.BridgedElement;
import org.hibernate.search.mapper.pojo.model.spi.BridgedElementModel;
import org.hibernate.search.mapper.pojo.model.spi.BridgedElementReader;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MappingDefinition;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.mapper.pojo.bridge.spi.RoutingKeyBridge;
import org.hibernate.search.engine.search.SearchQuery;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.json.JSONException;

import static org.hibernate.search.util.StubAssert.assertRequest;

/**
 * @author Yoann Rodiere
 */
public class JavaBeanElasticsearchRoutingIT {

	private SearchMappingRepository mappingRepository;

	private JavaBeanMapping mapping;

	private static final String HOST_1 = "http://es1.mycompany.com:9200/";

	@Before
	public void setup() throws JSONException {
		SearchMappingRepositoryBuilder mappingRepositoryBuilder = SearchMappingRepository.builder()
				.setProperty( "backend.elasticsearchBackend_1.type", ElasticsearchBackendFactory.class.getName() )
				.setProperty( "backend.elasticsearchBackend_1.host", HOST_1 )
				.setProperty( "index.default.backend", "elasticsearchBackend_1" );

		JavaBeanMappingContributor contributor = new JavaBeanMappingContributor( mappingRepositoryBuilder );

		MappingDefinition mappingDefinition = contributor.programmaticMapping();
		mappingDefinition.type( IndexedEntity.class )
				.indexed( IndexedEntity.INDEX )
				.routingKeyBridge( MyRoutingKeyBridge.class )
				.property( "id" )
						.documentId()
				.property( "value" ).field();

		mappingRepository = mappingRepositoryBuilder.build();
		mapping = contributor.getResult();

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();

		assertRequest( requests, IndexedEntity.INDEX, 0, HOST_1, "createIndex", null,
				"{"
					+ "'mapping': {"
						+ "'_routing': {"
							+ "'required': true"
						+ "},"
						+ "'properties': {"
							+ "'value': {"
								+ "'type': 'keyword'"
							+ "}"
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
		try (PojoSearchManager manager = mapping.createSearchManager()) {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setCategory( EntityCategory.CATEGORY_2 );
			entity1.setValue( "val1" );

			manager.getMainWorker().add( entity1 );
		}

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();
		assertRequest( requests, IndexedEntity.INDEX, 0, HOST_1, "add", "1",
				c -> {
					c.accept( "_routing", "category_2" );
				},
				"{"
					+ "'value': 'val1'"
				+ "}" );
	}

	@Test
	public void index_multiTenancy() throws JSONException {
		try (PojoSearchManager manager = mapping.createSearchManagerWithOptions()
				.tenantId( "myTenantId" )
				.build() ) {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setCategory( EntityCategory.CATEGORY_2 );
			entity1.setValue( "val1" );

			manager.getMainWorker().add( entity1 );
		}

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();
		assertRequest( requests, IndexedEntity.INDEX, 0, HOST_1, "add", "1",
				c -> {
					c.accept( "_routing", "myTenantId/category_2" );
				},
				"{"
						+ "'value': 'val1'"
						+ "}" );
	}

	@Test
	public void search() throws JSONException {
		try (PojoSearchManager manager = mapping.createSearchManager()) {
			SearchQuery<PojoReference> query = manager.search( IndexedEntity.class )
					.asReferences()
					.match()
							.onField( "value" )
							.matching( "val1" )
							.end()
					.routing( "category_2" )
					.build();

			StubElasticsearchClient.pushStubResponse(
					"{"
						+ "'hits': {"
							+ "'hits': ["
								+ "{"
									+ "'_index': '" + IndexedEntity.INDEX + "',"
									+ "'_id': '0'"
								+ "},"
								+ "{"
									+ "'_index': '" + IndexedEntity.INDEX + "',"
									+ "'_id': '1'"
								+ "}"
							+ "]"
						+ "}"
					+ "}"
			);
			query.execute();
		}

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();
		assertRequest( requests, IndexedEntity.INDEX, 0,
				HOST_1, "search", null /* No ID */,
				c -> {
					c.accept( "_routing", "category_2" );
				},
				"{"
					+ "'query': {"
						+ "'match': {"
							+ "'value': {"
								+ "'value': 'val1'"
							+ "}"
						+ "}"
					+ "}"
				+ "}" );
	}

	// TODO implement filters and allow them to use routing predicates, then test this here

	public enum EntityCategory {
		CATEGORY_1,
		CATEGORY_2;
	}

	public static final class IndexedEntity {

		public static final String INDEX = "IndexedEntity";

		private Integer id;

		private EntityCategory category;

		private String value;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityCategory getCategory() {
			return category;
		}

		public void setCategory(EntityCategory category) {
			this.category = category;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

	public static final class MyRoutingKeyBridge implements RoutingKeyBridge {

		private BridgedElementReader<EntityCategory> categoryReader;

		@Override
		public void bind(BridgedElementModel bridgedElementModel) {
			categoryReader = bridgedElementModel.property( "category" ).createReader( EntityCategory.class );
		}

		@Override
		public String apply(String tenantIdentifier, Object entityIdentifier, BridgedElement source) {
			EntityCategory category = categoryReader.read( source );
			StringBuilder keyBuilder = new StringBuilder();
			if ( tenantIdentifier != null ) {
				keyBuilder.append( tenantIdentifier ).append( "/" );
			}
			switch ( category ) {
				case CATEGORY_1:
					keyBuilder.append( "category_1" );
					break;
				case CATEGORY_2:
					keyBuilder.append( "category_2" );
					break;
				default:
					throw new RuntimeException( "Unknown category: " + category );
			}
			return keyBuilder.toString();
		}
	}

}

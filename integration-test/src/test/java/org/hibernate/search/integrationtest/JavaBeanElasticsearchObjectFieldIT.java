/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaObjectField;
import org.hibernate.search.backend.elasticsearch.client.impl.StubElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.impl.StubElasticsearchClient.Request;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchBackendFactory;
import org.hibernate.search.engine.common.SearchMappingRepository;
import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.JavaBeanMappingContributor;
import org.hibernate.search.engine.mapper.model.SearchModel;
import org.hibernate.search.mapper.pojo.bridge.Bridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingDefinition;
import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.PojoModelElement;
import org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.util.SearchException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.json.JSONException;

import static org.hibernate.search.integrationtest.util.StubAssert.assertRequest;

/**
 * @author Yoann Rodiere
 */
public class JavaBeanElasticsearchObjectFieldIT {

	private static final String HOST = "http://es1.mycompany.com:9200/";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private SearchMappingRepository mappingRepository;

	private JavaBeanMapping mapping;

	@Before
	public void setup() throws JSONException {
		SearchMappingRepositoryBuilder mappingRepositoryBuilder = SearchMappingRepository.builder()
				.setProperty( "backend.elasticsearchBackend.type", ElasticsearchBackendFactory.class.getName() )
				.setProperty( "backend.elasticsearchBackend.host", HOST )
				.setProperty( "index.default.backend", "elasticsearchBackend" );

		JavaBeanMappingContributor contributor = new JavaBeanMappingContributor( mappingRepositoryBuilder );

		ProgrammaticMappingDefinition mappingDefinition = contributor.programmaticMapping();
		mappingDefinition.type( IndexedEntity.class )
				.indexed( IndexedEntity.INDEX )
				.property( "id" )
						.documentId()
				.property( "texts" )
						.bridge(
								new MyBridgeBuilder()
										.objectName( "flattenedTexts" )
										.storage( ObjectFieldStorage.FLATTENED )
						)
						.bridge(
								new MyBridgeBuilder()
										.objectName( "nestedTexts" )
										.storage( ObjectFieldStorage.NESTED )
						);

		mappingRepository = mappingRepositoryBuilder.build();
		mapping = contributor.getResult();

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();

		assertRequest( requests, IndexedEntity.INDEX, 0, HOST, "createIndex", null,
				"{"
					+ "'mapping': {"
						+ "'properties': {"
							+ "'flattenedTexts': {"
								+ "'type': 'object',"
								+ "'properties': {"
									+ "'text': {"
										+ "'type': 'keyword'"
									+ "}"
								+ "}"
							+ "},"
							+ "'nestedTexts': {"
								+ "'type': 'nested',"
								+ "'properties': {"
									+ "'text': {"
										+ "'type': 'keyword'"
									+ "}"
								+ "}"
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
		try ( PojoSearchManager manager = mapping.createSearchManager() ) {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setTexts( Arrays.asList(
					"value1,value2",
					"value1",
					null,
					"value1,value2,value3"
			) );
			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setTexts( Collections.singletonList(
					"value2,value3"
			) );
			IndexedEntity entity3 = new IndexedEntity();
			entity3.setId( 3 );
			entity3.setTexts( Collections.emptyList() );

			manager.getMainWorker().add( entity1 );
			manager.getMainWorker().add( entity2 );
			manager.getMainWorker().add( entity3 );
		}

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();
		assertRequest( requests, IndexedEntity.INDEX, 0, HOST, "add", "1",
				"{"
					+ "'flattenedTexts': ["
						+ "{"
							+ "'text': ['value1', 'value2']"
						+ "},"
						+ "{"
							+ "'text': 'value1'"
						+ "},"
						+ "null,"
						+ "{"
							+ "'text': ['value1', 'value2', 'value3']"
						+ "}"
					+ "],"
					// Nested texts are identical here, but are stored differently by Elasticsearch
					+ "'nestedTexts': ["
						+ "{"
							+ "'text': ['value1', 'value2']"
						+ "},"
						+ "{"
							+ "'text': 'value1'"
						+ "},"
						+ "null,"
						+ "{"
							+ "'text': ['value1', 'value2', 'value3']"
						+ "}"
					+ "]"
				+ "}" );
		assertRequest( requests, IndexedEntity.INDEX, 1, HOST, "add", "2",
				"{"
					+ "'flattenedTexts': {"
							+ "'text': ['value2', 'value3']"
					+ "},"
					+ "'nestedTexts': {"
							+ "'text': ['value2', 'value3']"
					+ "}"
				+ "}" );
		assertRequest( requests, IndexedEntity.INDEX, 2, HOST, "add", "3",
				"{"
				+ "}" );
	}

	@Test
	public void search_nestedPredicate() throws JSONException {
		try ( PojoSearchManager manager = mapping.createSearchManager() ) {
			SearchQuery<PojoReference> query = manager.search( IndexedEntity.class )
					.query()
					.asReferences()
					.predicate().nested().onObjectField( "nestedTexts" ).bool( b -> {
						b.must().match()
								.onField( "nestedTexts.text" )
								.matching( "value_1" );
						b.must().match()
								.onField( "nestedTexts.text" )
								.matching( "value_3" );
					} )
					.build();

			query.execute();
		}

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();
		assertRequest( requests, IndexedEntity.INDEX, 0,
				HOST, "query", null /* No ID */,
				"{"
					+ "'query': {"
						+ "'nested': {"
							+ "'path': 'nestedTexts',"
							+ "'query': {"
								+ "'bool': {"
									+ "'must': ["
										+ "{"
											+ "'match': {"
												+ "'nestedTexts.text': {"
													+ "'query': 'value_1'"
												+ "}"
											+ "}"
										+ "},"
										+ "{"
											+ "'match': {"
												+ "'nestedTexts.text': {"
													+ "'query': 'value_3'"
												+ "}"
											+ "}"
										+ "}"
									+ "]"
								+ "}"
							+ "}"
						+ "}"
					+ "}"
				+ "}" );
	}

	@Test
	public void nestedPredicate_error_nonNestedField() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "'flattenedTexts'" );
		thrown.expectMessage( "is not stored as nested" );
		try ( PojoSearchManager manager = mapping.createSearchManager() ) {
			manager.search( IndexedEntity.class )
					.predicate().nested().onObjectField( "flattenedTexts" );
		}
	}

	@Test
	public void nestedPredicate_error_nonObjectField() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "'flattenedTexts.text'" );
		thrown.expectMessage( "is not an object field" );
		try ( PojoSearchManager manager = mapping.createSearchManager() ) {
			manager.search( IndexedEntity.class )
					.predicate().nested().onObjectField( "flattenedTexts.text" );
		}
	}

	@Test
	public void nestedPredicate_error_missingField() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field 'doesNotExist'" );
		try ( PojoSearchManager manager = mapping.createSearchManager() ) {
			manager.search( IndexedEntity.class )
					.predicate().nested().onObjectField( "doesNotExist" );
		}
	}

	public static final class IndexedEntity {

		public static final String INDEX = "IndexedEntity";

		private Integer id;

		private List<String> texts;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<String> getTexts() {
			return texts;
		}

		public void setTexts(List<String> texts) {
			this.texts = texts;
		}

	}

	public static final class MyBridgeBuilder implements BridgeBuilder<Bridge> {

		private String objectName;
		private ObjectFieldStorage storage = ObjectFieldStorage.DEFAULT;

		public MyBridgeBuilder objectName(String value) {
			this.objectName = value;
			return this;
		}

		public MyBridgeBuilder storage(ObjectFieldStorage storage) {
			this.storage = storage;
			return this;
		}

		@Override
		public Bridge build(BuildContext buildContext) {
			return new MyBridgeImpl( objectName, storage );
		}
	}

	private static final class MyBridgeImpl implements Bridge {

		private final String objectName;
		private final ObjectFieldStorage storage;
		private PojoModelElementAccessor<List> sourceAccessor;
		private IndexObjectFieldAccessor objectFieldAccessor;
		private IndexFieldAccessor<String> textFieldAccessor;

		public MyBridgeImpl(String objectName, ObjectFieldStorage storage) {
			this.objectName = objectName;
			this.storage = storage;
		}

		@Override
		public void bind(IndexSchemaElement indexSchemaElement, PojoModelElement bridgedPojoModelElement,
				SearchModel searchModel) {
			sourceAccessor = bridgedPojoModelElement.createAccessor( List.class );
			IndexSchemaObjectField objectField = indexSchemaElement.objectField( objectName, storage );
			objectFieldAccessor = objectField.createAccessor();
			textFieldAccessor = objectField.field( "text" ).asString().createAccessor();
		}

		@Override
		public void write(DocumentElement target, PojoElement source) {
			List<String> sourceValue = sourceAccessor.read( source );
			if ( sourceValue != null ) {
				for ( String item : sourceValue ) {
					if ( item != null ) {
						String[] subItems = item.split( "," );
						DocumentElement object = objectFieldAccessor.add( target );
						for ( String subItem : subItems ) {
							textFieldAccessor.write( object, subItem );
						}
					}
					else {
						objectFieldAccessor.addMissing( target );
					}
				}
			}
		}

	}

}

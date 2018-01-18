/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.backend.document.model.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.DocumentState;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.backend.elasticsearch.client.impl.StubElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.impl.StubElasticsearchClient.Request;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchBackendFactory;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchDocumentReference;
import org.hibernate.search.engine.common.SearchMappingRepository;
import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.JavaBeanMappingContributor;
import org.hibernate.search.engine.mapper.model.SearchModel;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultIntegerIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.Bridge;
import org.hibernate.search.mapper.pojo.bridge.FunctionBridge;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchTarget;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MappingDefinition;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoReferenceImpl;
import org.hibernate.search.mapper.pojo.model.PojoModelElement;
import org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoState;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.ProjectionConstants;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.SearchResult;
import org.hibernate.search.engine.search.dsl.predicate.RangeBoundInclusion;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.fest.assertions.Assertions;
import org.json.JSONException;

import static org.hibernate.search.util.StubAssert.assertRequest;

/**
 * @author Yoann Rodiere
 */
public class JavaBeanElasticsearchIT {

	private SearchMappingRepository mappingRepository;

	private JavaBeanMapping mapping;

	private static final String HOST_1 = "http://es1.mycompany.com:9200/";
	private static final String HOST_2 = "http://es2.mycompany.com:9200/";

	@Before
	public void setup() throws JSONException {
		SearchMappingRepositoryBuilder mappingRepositoryBuilder = SearchMappingRepository.builder()
				.setProperty( "backend.elasticsearchBackend_1.type", ElasticsearchBackendFactory.class.getName() )
				.setProperty( "backend.elasticsearchBackend_1.host", HOST_1 )
				.setProperty( "backend.elasticsearchBackend_2.type", ElasticsearchBackendFactory.class.getName() )
				.setProperty( "backend.elasticsearchBackend_2.host", HOST_2 )
				.setProperty( "index.default.backend", "elasticsearchBackend_1" )
				.setProperty( "index.OtherIndexedEntity.backend", "elasticsearchBackend_2" );

		JavaBeanMappingContributor contributor = new JavaBeanMappingContributor( mappingRepositoryBuilder );

		MappingDefinition mappingDefinition = contributor.programmaticMapping();
		mappingDefinition.type( IndexedEntity.class )
				.indexed( IndexedEntity.INDEX )
				.bridge(
						new MyBridgeBuilder()
						.objectName( "customBridgeOnClass" )
				)
				.property( "id" )
						.documentId()
				.property( "text" )
						.field()
								.name( "myTextField" )
				.property( "embedded" )
						.indexedEmbedded()
								.maxDepth( 1 )
								.includePaths( "embedded.customBridgeOnClass.text" );

		MappingDefinition secondMappingDefinition = contributor.programmaticMapping();
		secondMappingDefinition.type( ParentIndexedEntity.class )
				.property( "localDate" )
						.field()
								.name( "myLocalDateField" )
				.property( "embedded" )
						.bridge(
								new MyBridgeBuilder()
								.objectName( "customBridgeOnProperty" )
						);
		secondMappingDefinition.type( OtherIndexedEntity.class )
				.indexed( OtherIndexedEntity.INDEX )
				.property( "id" )
						.documentId().identifierBridge( DefaultIntegerIdentifierBridge.class )
				.property( "numeric" )
						.field()
						.field().name( "numericAsString" ).functionBridge( IntegerAsStringFunctionBridge.class );
		secondMappingDefinition.type( YetAnotherIndexedEntity.class )
				.indexed( YetAnotherIndexedEntity.INDEX )
				.property( "id" )
						.documentId()
				.property( "numeric" )
						.field();

		mappingRepository = mappingRepositoryBuilder.build();
		mapping = contributor.getResult();

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();

		assertRequest( requests, OtherIndexedEntity.INDEX, 0, HOST_2, "createIndex", null,
				"{"
					+ "'mapping': {"
						+ "'properties': {"
							+ "'numeric': {"
								+ "'type': 'integer'"
							+ "},"
							+ "'numericAsString': {"
								+ "'type': 'keyword'"
							+ "}"
						+ "}"
					+ "}"
				+ "}" );
		assertRequest( requests, YetAnotherIndexedEntity.INDEX, 0, HOST_1, "createIndex", null,
				"{"
					+ "'mapping': {"
						+ "'properties': {"
							+ "'customBridgeOnProperty': {"
								+ "'type': 'object',"
								+ "'properties': {"
									+ "'date': {"
										+ "'type': 'date',"
										+ "'format': 'strict_date||yyyyyyyyy-MM-dd'"
									+ "},"
									+ "'text': {"
										+ "'type': 'keyword'"
									+ "}"
								+ "}"
							+ "},"
							+ "'myLocalDateField': {"
								+ "'type': 'date',"
								+ "'format': 'strict_date||yyyyyyyyy-MM-dd'"
							+ "},"
							+ "'numeric': {"
								+ "'type': 'integer'"
							+ "},"
						+ "}"
					+ "}"
				+ "}" );
		assertRequest( requests, IndexedEntity.INDEX, 0, HOST_1, "createIndex", null,
				"{"
					+ "'mapping': {"
						+ "'properties': {"
							+ "'customBridgeOnClass': {"
								+ "'type': 'object',"
								+ "'properties': {"
									+ "'date': {"
										+ "'type': 'date',"
										+ "'format': 'strict_date||yyyyyyyyy-MM-dd'"
									+ "},"
									+ "'text': {"
										+ "'type': 'keyword'"
									+ "}"
								+ "}"
							+ "},"
							+ "'customBridgeOnProperty': {"
								+ "'type': 'object',"
								+ "'properties': {"
									+ "'date': {"
										+ "'type': 'date',"
										+ "'format': 'strict_date||yyyyyyyyy-MM-dd'"
									+ "},"
									+ "'text': {"
										+ "'type': 'keyword'"
									+ "}"
								+ "}"
							+ "},"
							+ "'embedded': {"
								+ "'type': 'object',"
								+ "'properties': {"
									+ "'customBridgeOnClass': {"
										+ "'type': 'object',"
										+ "'properties': {"
											+ "'date': {"
												+ "'type': 'date',"
												+ "'format': 'strict_date||yyyyyyyyy-MM-dd'"
											+ "},"
											+ "'text': {"
												+ "'type': 'keyword'"
											+ "}"
										+ "}"
									+ "},"
									+ "'customBridgeOnProperty': {"
										+ "'type': 'object',"
										+ "'properties': {"
											+ "'date': {"
												+ "'type': 'date',"
												+ "'format': 'strict_date||yyyyyyyyy-MM-dd'"
											+ "},"
											+ "'text': {"
												+ "'type': 'keyword'"
											+ "}"
										+ "}"
									+ "},"
									+ "'embedded': {"
										+ "'type': 'object',"
										+ "'properties': {"
											+ "'customBridgeOnClass': {"
												+ "'type': 'object',"
												+ "'properties': {"
													+ "'text': {"
														+ "'type': 'keyword'"
													+ "}"
												+ "}"
											+ "}"
										+ "}"
									+ "},"
									+ "'myLocalDateField': {"
										+ "'type': 'date',"
										+ "'format': 'strict_date||yyyyyyyyy-MM-dd'"
									+ "},"
									+ "'myTextField': {"
										+ "'type': 'keyword'"
									+ "}"
								+ "}"
							+ "},"
							+ "'myLocalDateField': {"
								+ "'type': 'date',"
								+ "'format': 'strict_date||yyyyyyyyy-MM-dd'"
							+ "},"
							+ "'myTextField': {"
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
			entity1.setText( "this is text (1)" );
			entity1.setLocalDate( LocalDate.of( 2017, 11, 1 ) );
			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setText( "some more text (2)" );
			entity2.setLocalDate( LocalDate.of( 2017, 11, 2 ) );
			IndexedEntity entity3 = new IndexedEntity();
			entity3.setId( 3 );
			entity3.setText( "some more text (3)" );
			entity3.setLocalDate( LocalDate.of( 2017, 11, 3 ) );
			OtherIndexedEntity entity4 = new OtherIndexedEntity();
			entity4.setId( 4 );
			entity4.setNumeric( 404 );

			entity1.setEmbedded( entity2 );
			entity2.setEmbedded( entity3 );

			manager.getMainWorker().add( entity1 );
			manager.getMainWorker().add( entity2 );
			manager.getMainWorker().add( entity4 );
			manager.getMainWorker().delete( entity1 );
			manager.getMainWorker().add( entity3 );
		}

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();
		// We expect the first add to be removed due to the delete
		assertRequest( requests, IndexedEntity.INDEX, 0, HOST_1, "add", "2",
				"{"
					+ "'customBridgeOnClass': {"
						+ "'text': 'some more text (2)',"
						+ "'date': '2017-11-02'"
					+ "},"
					+ "'myLocalDateField': '2017-11-02',"
					+ "'customBridgeOnProperty': {"
						+ "'text': 'some more text (3)',"
						+ "'date': '2017-11-03'"
					+ "},"
					+ "'embedded': {"
						+ "'customBridgeOnClass': {"
							+ "'text': 'some more text (3)',"
							+ "'date': '2017-11-03'"
						+ "},"
						+ "'myLocalDateField': '2017-11-03',"
						+ "'myTextField': 'some more text (3)'"
					+ "},"
					+ "'myTextField': 'some more text (2)'"
				+ "}" );
		assertRequest( requests, IndexedEntity.INDEX, 1, HOST_1, "add", "3",
				"{"
					+ "'customBridgeOnClass': {"
						+ "'text': 'some more text (3)',"
						+ "'date': '2017-11-03'"
					+ "},"
					+ "'myLocalDateField': '2017-11-03',"
					+ "'myTextField': 'some more text (3)'"
				+ "}" );
		assertRequest( requests, OtherIndexedEntity.INDEX, 0, HOST_2, "add", "4",
				"{"
					+ "'numeric': 404,"
					+ "'numericAsString': '404'"
				+ "}" );
	}

	@Test
	public void search() throws JSONException {
		try (PojoSearchManager manager = mapping.createSearchManager()) {
			SearchQuery<PojoReference> query = manager.search(
							Arrays.asList( IndexedEntity.class, YetAnotherIndexedEntity.class )
					)
					.query()
					.asReferences()
					.predicate( root -> root.bool()
							.must().match()
									.onField( "myTextField" ).boostedTo( 1.5f )
									.orField( "embedded.myTextField" ).boostedTo( 0.9f )
									.matching( "foo" )
							.should().range()
									.onField( "myLocalDateField" )
									.from( LocalDate.of( 2017, 10, 01 ), RangeBoundInclusion.EXCLUDED )
									.to( LocalDate.of( 2017, 11, 01 ) )
							/*
							 * Alternative syntax taking advantage of lambdas,
							 * allowing to introduce if/else statements in the query building code
							 * and removing the need to call .end() on nested clause contexts.
							 */
							.should( c -> {
								if ( /* put some condition here, for example based on user input */ true ) {
									c.range()
											.onField( "numeric" )
											.above( 12 );
								}
							} )
					)
					.build();
			query.setFirstResult( 2L );
			query.setMaxResults( 10L );

			StubElasticsearchClient.pushStubResponse(
					"{"
						+ "'hits': {"
							+ "'hits': ["
								+ "{"
									+ "'_index': '" + IndexedEntity.INDEX + "',"
									+ "'_id': '0'"
								+ "},"
								+ "{"
									+ "'_index': '" + YetAnotherIndexedEntity.INDEX + "',"
									+ "'_id': '1'"
								+ "}"
							+ "]"
						+ "}"
					+ "}"
			);
			SearchResult<PojoReference> result = query.execute();
			Assertions.assertThat( result.getHits() ).hasSize( 2 )
					.containsExactly(
							new PojoReferenceImpl( IndexedEntity.class, 0 ),
							new PojoReferenceImpl( YetAnotherIndexedEntity.class, 1 )
					);
			Assertions.assertThat( result.getHitCount() ).isEqualTo( 2 );
		}

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();
		assertRequest( requests, Arrays.asList( IndexedEntity.INDEX, YetAnotherIndexedEntity.INDEX ), 0,
				HOST_1, "query", null /* No ID */,
				c -> {
					c.accept( "offset", "2" );
					c.accept( "limit", "10" );
				},
				"{"
					+ "'query': {"
						+ "'bool': {"
							+ "'must': {"
								+ "'bool': {"
									+ "'should': ["
										+ "{"
											+ "'match': {"
												+ "'myTextField': {"
													+ "'value': 'foo',"
													+ "'boost': 1.5"
												+ "}"
											+ "}"
										+ "},"
										+ "{"
											+ "'match': {"
												+ "'embedded.myTextField': {"
													+ "'value': 'foo',"
													+ "'boost': 0.9"
												+ "}"
											+ "}"
										+ "}"
									+ "]"
								+ "}"
							+ "},"
							+ "'should': ["
								+ "{"
									+ "'range': {"
										+ "'myLocalDateField': {"
											+ "'gt': '2017-10-01',"
											+ "'lte': '2017-11-01'"
										+ "}"
									+ "}"
								+ "},"
								+ "{"
									+ "'range': {"
										+ "'numeric': {"
											+ "'gte': 12"
										+ "}"
									+ "}"
								+ "}"
							+ "]"
						+ "}"
					+ "}"
				+ "}" );
	}

	@Test
	public void search_separatePredicate() throws JSONException {
		try (PojoSearchManager manager = mapping.createSearchManager()) {
			PojoSearchTarget<?> target = manager.search(
					Arrays.asList( IndexedEntity.class, YetAnotherIndexedEntity.class )
			);

			SearchPredicate nestedPredicate = target.predicate()
					.range()
							.onField( "myLocalDateField" )
							.from( LocalDate.of( 2017, 10, 01 ), RangeBoundInclusion.EXCLUDED )
							.to( LocalDate.of( 2017, 11, 01 ) );
			SearchPredicate otherNestedPredicate = target.predicate()
					.range()
							.onField( "numeric" )
							.above( 12 );
			SearchPredicate predicate = target.predicate()
					.bool()
							.must().match()
									.onField( "myTextField" ).boostedTo( 1.5f )
									.orField( "embedded.myTextField" ).boostedTo( 0.9f )
									.matching( "foo" )
							.should( nestedPredicate )
							.mustNot().predicate( otherNestedPredicate )
					.end();
			SearchQuery<PojoReference> query = target.query()
					.asReferences()
					.predicate( predicate )
					.build();
			query.setFirstResult( 2L );
			query.setMaxResults( 10L );

			StubElasticsearchClient.pushStubResponse(
					"{"
						+ "'hits': {"
							+ "'hits': ["
								+ "{"
									+ "'_index': '" + IndexedEntity.INDEX + "',"
									+ "'_id': '0'"
								+ "},"
								+ "{"
									+ "'_index': '" + YetAnotherIndexedEntity.INDEX + "',"
									+ "'_id': '1'"
								+ "}"
							+ "]"
						+ "}"
					+ "}"
			);
			query.execute();
		}

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();
		assertRequest( requests, Arrays.asList( IndexedEntity.INDEX, YetAnotherIndexedEntity.INDEX ), 0,
				HOST_1, "query", null /* No ID */,
				c -> {
					c.accept( "offset", "2" );
					c.accept( "limit", "10" );
				},
				"{"
					+ "'query': {"
						+ "'bool': {"
							+ "'must': {"
								+ "'bool': {"
									+ "'should': ["
										+ "{"
											+ "'match': {"
												+ "'myTextField': {"
													+ "'value': 'foo',"
													+ "'boost': 1.5"
												+ "}"
											+ "}"
										+ "},"
										+ "{"
											+ "'match': {"
												+ "'embedded.myTextField': {"
													+ "'value': 'foo',"
													+ "'boost': 0.9"
												+ "}"
											+ "}"
										+ "}"
									+ "]"
								+ "}"
							+ "},"
							+ "'should': {"
								+ "'range': {"
									+ "'myLocalDateField': {"
										+ "'gt': '2017-10-01',"
										+ "'lte': '2017-11-01'"
									+ "}"
								+ "}"
							+ "},"
							+ "'must_not': {"
								+ "'range': {"
									+ "'numeric': {"
										+ "'gte': 12"
									+ "}"
								+ "}"
							+ "}"
						+ "}"
					+ "}"
				+ "}" );
	}

	@Test
	public void search_projection() throws JSONException {
		try (PojoSearchManager manager = mapping.createSearchManager()) {
			SearchQuery<List<?>> query = manager.search(
							Arrays.asList( IndexedEntity.class, YetAnotherIndexedEntity.class )
					)
					.query()
					.asProjections(
							"myTextField",
							ProjectionConstants.REFERENCE,
							"myLocalDateField",
							ProjectionConstants.DOCUMENT_REFERENCE,
							"customBridgeOnClass.text"
					)
					.predicate( root -> root.match()
							.onField( "myTextField" )
							.matching( "foo" )
					)
					.build();

			StubElasticsearchClient.pushStubResponse(
					"{"
						+ "'hits': {"
							+ "'hits': ["
								+ "{"
									+ "'_index': '" + IndexedEntity.INDEX + "',"
									+ "'_id': '0',"
									+ "_source: {"
										+ "'myLocalDateField': '2017-11-01',"
										+ "'myTextField': 'text1',"
										+ "'customBridgeOnClass.text': 'text2'"
									+ "}"
								+ "},"
								+ "{"
									+ "'_index': '" + YetAnotherIndexedEntity.INDEX + "',"
									+ "'_id': '1',"
									+ "_source: {"
										+ "'myLocalDateField': '2017-11-02'"
									+ "}"
								+ "}"
							+ "]"
						+ "}"
					+ "}"
			);
			SearchResult<List<?>> result = query.execute();
			Assertions.assertThat( result.getHits() ).hasSize( 2 )
					.containsExactly(
							Arrays.asList(
									"text1",
									new PojoReferenceImpl( IndexedEntity.class, 0 ),
									LocalDate.of( 2017, 11, 1 ),
									new ElasticsearchDocumentReference( IndexedEntity.INDEX, "0" ),
									"text2"
							),
							Arrays.asList(
									null,
									new PojoReferenceImpl( YetAnotherIndexedEntity.class, 1 ),
									LocalDate.of( 2017, 11, 2 ),
									new ElasticsearchDocumentReference( YetAnotherIndexedEntity.INDEX, "1" ),
									null
							)
					);
			Assertions.assertThat( result.getHitCount() ).isEqualTo( 2 );
		}

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();
		assertRequest( requests, Arrays.asList( IndexedEntity.INDEX, YetAnotherIndexedEntity.INDEX ), 0,
				HOST_1, "query", null /* No ID */,
				null,
				"{"
					+ "'query': {"
						+ "'match': {"
							+ "'myTextField': {"
								+ "'value': 'foo'"
							+ "}"
						+ "}"
					+ "},"
					+ "'_source': ["
						+ "'myTextField',"
						+ "'myLocalDateField',"
						+ "'customBridgeOnClass.text'"
					+ "]"
				+ "}" );
	}

	public static class ParentIndexedEntity {

		private LocalDate localDate;

		private IndexedEntity embedded;

		public LocalDate getLocalDate() {
			return localDate;
		}

		public void setLocalDate(LocalDate localDate) {
			this.localDate = localDate;
		}

		public IndexedEntity getEmbedded() {
			return embedded;
		}

		public void setEmbedded(IndexedEntity embedded) {
			this.embedded = embedded;
		}

	}

	public static final class IndexedEntity extends ParentIndexedEntity {

		public static final String INDEX = "IndexedEntity";

		// TODO make it work with a primitive int too
		private Integer id;

		private String text;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

	}

	public static final class OtherIndexedEntity {

		public static final String INDEX = "OtherIndexedEntity";

		// TODO make it work with a primitive int too
		private Integer id;

		private Integer numeric;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer getNumeric() {
			return numeric;
		}

		public void setNumeric(Integer numeric) {
			this.numeric = numeric;
		}

	}

	public static final class YetAnotherIndexedEntity extends ParentIndexedEntity {

		public static final String INDEX = "YetAnotherIndexedEntity";

		private Integer id;

		private Integer numeric;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer getNumeric() {
			return numeric;
		}

		public void setNumeric(Integer numeric) {
			this.numeric = numeric;
		}
	}

	public static final class IntegerAsStringFunctionBridge implements FunctionBridge<Integer, String> {
		@Override
		public String toIndexedValue(Integer propertyValue) {
			return propertyValue == null ? null : propertyValue.toString();
		}
	}

	public static final class MyBridgeBuilder implements BridgeBuilder<Bridge> {

		private String objectName;

		public MyBridgeBuilder objectName(String value) {
			this.objectName = value;
			return this;
		}

		@Override
		public Bridge build(BuildContext buildContext) {
			return new MyBridgeImpl( objectName );
		}
	}

	private static final class MyBridgeImpl implements Bridge {

		private final String objectName;

		private PojoModelElementAccessor<IndexedEntity> sourceAccessor;
		private IndexFieldAccessor<String> textFieldAccessor;
		private IndexFieldAccessor<LocalDate> localDateFieldAccessor;

		MyBridgeImpl(String objectName) {
			this.objectName = objectName;
		}

		@Override
		public void contribute(IndexSchemaElement indexSchemaElement, PojoModelElement bridgedPojoModelElement,
				SearchModel searchModel) {
			sourceAccessor = bridgedPojoModelElement.createAccessor( IndexedEntity.class );
			IndexSchemaElement objectFieldMetadata = indexSchemaElement.objectField( objectName );
			textFieldAccessor = objectFieldMetadata.field( "text" ).asString().createAccessor();
			localDateFieldAccessor = objectFieldMetadata.field( "date" ).asLocalDate().createAccessor();
		}

		@Override
		public void write(DocumentState target, PojoState source) {
			IndexedEntity sourceValue = sourceAccessor.read( source );
			if ( sourceValue != null ) {
				textFieldAccessor.write( target, sourceValue.getText() );
				localDateFieldAccessor.write( target, sourceValue.getLocalDate() );
			}
		}

	}

}

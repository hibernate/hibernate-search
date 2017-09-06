/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search;

import static org.hibernate.search.util.StubAssert.assertRequest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.fest.assertions.Assertions;
import org.hibernate.search.engine.backend.document.model.spi.IndexModelCollector;
import org.hibernate.search.engine.backend.document.spi.DocumentState;
import org.hibernate.search.engine.backend.document.spi.IndexFieldReference;
import org.hibernate.search.backend.elasticsearch.client.impl.StubElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.impl.StubElasticsearchClient.Request;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchBackendFactory;
import org.hibernate.search.engine.bridge.builtin.impl.DefaultIntegerIdentifierBridge;
import org.hibernate.search.engine.bridge.declaration.spi.BridgeBeanReference;
import org.hibernate.search.engine.bridge.declaration.spi.BridgeMapping;
import org.hibernate.search.engine.bridge.mapping.BridgeDefinitionBase;
import org.hibernate.search.engine.bridge.spi.Bridge;
import org.hibernate.search.engine.bridge.spi.FunctionBridge;
import org.hibernate.search.engine.common.SearchManagerFactory;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.mapper.javabean.mapping.JavaBeanMapper;
import org.hibernate.search.mapper.javabean.mapping.JavaBeanMappingType;
import org.hibernate.search.engine.mapper.model.spi.Indexable;
import org.hibernate.search.engine.mapper.model.spi.IndexableModel;
import org.hibernate.search.engine.mapper.model.spi.IndexableReference;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MappingDefinition;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoReferenceImpl;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.SearchResult;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
public class PojoElasticsearchIT {

	private SearchManagerFactory managerFactory;

	private static final String HOST_1 = "http://es1.mycompany.com:9200/";
	private static final String HOST_2 = "http://es2.mycompany.com:9200/";

	@Before
	public void setup() throws JSONException {
		MappingDefinition mapping = JavaBeanMapper.get().programmaticMapping();
		mapping.type( IndexedEntity.class )
				.indexed( IndexedEntity.INDEX )
				.bridge(
						new MyBridgeDefinition()
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

		MappingDefinition secondMapping = JavaBeanMapper.get().programmaticMapping();
		secondMapping.type( ParentIndexedEntity.class )
				.property( "localDate" )
						.field()
								.name( "myLocalDateField" )
				.property( "embedded" )
						.bridge(
								new MyBridgeDefinition()
								.objectName( "customBridgeOnProperty" )
						);
		secondMapping.type( OtherIndexedEntity.class )
				.indexed( OtherIndexedEntity.INDEX )
				.property( "id" )
						.documentId().bridge( DefaultIntegerIdentifierBridge.class )
				.property( "numeric" )
						.field()
						.field().name( "numericAsString" ).bridge( IntegerAsStringFunctionBridge.class );
		secondMapping.type( YetAnotherIndexedEntity.class )
				.indexed( YetAnotherIndexedEntity.INDEX )
				.property( "id" )
						.documentId()
				.property( "numeric" )
						.field();

		managerFactory = SearchManagerFactory.builder()
				.addMapping( mapping )
				.addMapping( secondMapping )
				.setProperty( "backend.elasticsearchBackend_1.type", ElasticsearchBackendFactory.class.getName() )
				.setProperty( "backend.elasticsearchBackend_1.host", HOST_1 )
				.setProperty( "backend.elasticsearchBackend_2.type", ElasticsearchBackendFactory.class.getName() )
				.setProperty( "backend.elasticsearchBackend_2.host", HOST_2 )
				.setProperty( "index.default.backend", "elasticsearchBackend_1" )
				.setProperty( "index.OtherIndexedEntity.backend", "elasticsearchBackend_2" )
				.build();

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
		if ( managerFactory != null ) {
			managerFactory.close();
		}
	}

	@Test
	public void index() throws JSONException {
		try ( PojoSearchManager manager = managerFactory.createSearchManager( JavaBeanMappingType.get() ) ) {
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

			manager.getWorker().add( entity1 );
			manager.getWorker().add( entity2 );
			manager.getWorker().add( entity4 );
			manager.getWorker().delete( IndexedEntity.class, 1 );
			manager.getWorker().add( entity3 );
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
		try ( PojoSearchManager manager = managerFactory.createSearchManager( JavaBeanMappingType.get() ) ) {
			SearchQuery<PojoReference> query = manager.search( IndexedEntity.class, YetAnotherIndexedEntity.class )
					.asReferences()
					.bool()
							.must().match()
									.onField( "myTextField" ).boostedTo( 1.5f )
									.orField( "embedded.myTextField" ).boostedTo( 0.9f )
									.matching( "foo" )
									.end()
							.should().range()
									.onField( "myLocalDateField" )
									.from( LocalDate.of( 2017, 10, 01 ) ).excludeLimit()
									.to( LocalDate.of( 2017, 11, 01 ) )
									.end()
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
							.end()
					.build();
			query.setFirstResult( 2L );
			query.setMaxResults( 10L );

			/*
			 * These are stubbed results, they are unrelated to the query. See StubElasticsearchWorkFactory.
			 * Here we just check that result wrapping (document reference -> pojo reference) works as expected.
			 */
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
				HOST_1, "search", null /* No ID */,
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
		public String toDocument(Integer propertyValue) {
			return propertyValue == null ? null : propertyValue.toString();
		}
	}

	@BridgeMapping(implementation = @BridgeBeanReference(type = MyBridgeImpl.class))
	@Target(value = { ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface MyBridge {
		String objectName();
	}

	public static final class MyBridgeDefinition extends BridgeDefinitionBase<MyBridge> {

		@Override
		protected Class<MyBridge> getAnnotationClass() {
			return MyBridge.class;
		}

		public MyBridgeDefinition objectName(String value) {
			addParameter( "objectName", value );
			return this;
		}
	}

	public static final class MyBridgeImpl implements Bridge<MyBridge> {

		private MyBridge parameters;
		private IndexableReference<IndexedEntity> sourceRef;
		private IndexFieldReference<String> textFieldRef;
		private IndexFieldReference<LocalDate> localDateFieldRef;

		@Override
		public void initialize(BuildContext buildContext, MyBridge parameters) {
			this.parameters = parameters;
		}

		@Override
		public void bind(IndexableModel indexableModel, IndexModelCollector indexModelCollector) {
			sourceRef = indexableModel.asReference( IndexedEntity.class );
			IndexModelCollector objectRef = indexModelCollector.childObject( parameters.objectName() );
			textFieldRef = objectRef.field( "text" ).fromString().asReference();
			localDateFieldRef = objectRef.field( "date" ).fromLocalDate().asReference();
		}

		@Override
		public void toDocument(Indexable source, DocumentState target) {
			IndexedEntity sourceValue = source.get( sourceRef );
			if ( sourceValue != null ) {
				textFieldRef.add( target, sourceValue.getText() );
				localDateFieldRef.add( target, sourceValue.getLocalDate() );
			}
		}

	}

}

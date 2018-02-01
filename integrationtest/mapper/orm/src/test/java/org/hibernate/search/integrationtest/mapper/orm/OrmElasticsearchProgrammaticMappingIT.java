/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaObjectField;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchDocumentReference;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.model.SearchModel;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.SearchOrmSettings;
import org.hibernate.search.mapper.orm.hibernate.FullTextQuery;
import org.hibernate.search.mapper.orm.hibernate.FullTextSearchTarget;
import org.hibernate.search.mapper.orm.hibernate.FullTextSession;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingContributor;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingContributor;
import org.hibernate.search.mapper.pojo.bridge.Bridge;
import org.hibernate.search.mapper.pojo.bridge.FunctionBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultIntegerIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingDefinition;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoReferenceImpl;
import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.PojoModelElement;
import org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor;
import org.hibernate.search.integrationtest.util.orm.OrmUtils;
import org.hibernate.search.integrationtest.util.common.StubClientElasticsearchBackendFactory;
import org.hibernate.search.integrationtest.util.common.StubElasticsearchClient;
import org.hibernate.search.integrationtest.util.common.StubElasticsearchClient.Request;
import org.hibernate.search.engine.search.ProjectionConstants;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.predicate.RangeBoundInclusion;
import org.hibernate.service.ServiceRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.fest.assertions.Assertions;
import org.json.JSONException;

import static org.hibernate.search.integrationtest.util.common.StubAssert.assertDropAndCreateIndexRequests;
import static org.hibernate.search.integrationtest.util.common.StubAssert.assertIndexDocumentRequest;
import static org.hibernate.search.integrationtest.util.common.StubAssert.assertRequest;

/**
 * @author Yoann Rodiere
 */
public class OrmElasticsearchProgrammaticMappingIT {

	private static final String PREFIX = SearchOrmSettings.PREFIX;

	private static final String HOST_1 = "http://es1.mycompany.com:9200/";
	private static final String HOST_2 = "http://es2.mycompany.com:9200/";

	private SessionFactory sessionFactory;

	@Before
	public void setup() throws JSONException {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
				.applySetting( PREFIX + "backend.elasticsearchBackend_1.type", StubClientElasticsearchBackendFactory.class.getName() )
				.applySetting( PREFIX + "backend.elasticsearchBackend_1.host", HOST_1 )
				.applySetting( PREFIX + "backend.elasticsearchBackend_2.type", StubClientElasticsearchBackendFactory.class.getName() )
				.applySetting( PREFIX + "backend.elasticsearchBackend_2.host", HOST_2 )
				.applySetting( PREFIX + "index.default.backend", "elasticsearchBackend_1" )
				.applySetting( PREFIX + "index.OtherIndexedEntity.backend", "elasticsearchBackend_2" )
				.applySetting( SearchOrmSettings.MAPPING_CONTRIBUTOR, new MyMappingContributor() );

		ServiceRegistry serviceRegistry = registryBuilder.build();

		MetadataSources ms = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( IndexedEntity.class )
				.addAnnotatedClass( ParentIndexedEntity.class )
				.addAnnotatedClass( OtherIndexedEntity.class )
				.addAnnotatedClass( YetAnotherIndexedEntity.class );

		Metadata metadata = ms.buildMetadata();

		final SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();
		this.sessionFactory = sfb.build();

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();

		assertDropAndCreateIndexRequests( requests, OtherIndexedEntity.INDEX, HOST_2,
				"{"
					+ "'properties': {"
						+ "'numeric': {"
							+ "'type': 'integer'"
						+ "},"
						+ "'numericAsString': {"
							+ "'type': 'keyword'"
						+ "}"
					+ "}"
				+ "}" );
		assertDropAndCreateIndexRequests( requests, YetAnotherIndexedEntity.INDEX, HOST_1,
				"{"
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
				+ "}" );
		assertDropAndCreateIndexRequests( requests, IndexedEntity.INDEX, HOST_1,
				"{"
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
								+ "'prefix_customBridgeOnClass': {"
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
								+ "'prefix_customBridgeOnProperty': {"
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
								+ "'prefix_embedded': {"
									+ "'type': 'object',"
									+ "'properties': {"
										+ "'prefix_customBridgeOnClass': {"
											+ "'type': 'object',"
											+ "'properties': {"
												+ "'text': {"
													+ "'type': 'keyword'"
												+ "}"
											+ "}"
										+ "}"
									+ "}"
								+ "},"
								+ "'prefix_myLocalDateField': {"
									+ "'type': 'date',"
									+ "'format': 'strict_date||yyyyyyyyy-MM-dd'"
								+ "},"
								+ "'prefix_myTextField': {"
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
				+ "}" );
	}

	@After
	public void cleanup() {
		StubElasticsearchClient.drainRequestsByIndex();
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	@Test
	public void index() throws JSONException {
		OrmUtils.withinTransaction( sessionFactory, session -> {
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

			session.persist( entity1 );
			session.persist( entity2 );
			session.persist( entity4 );
			session.delete( entity1 );
			session.persist( entity3 );
		} );

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();
		// We expect the first add to be removed due to the delete
		assertIndexDocumentRequest( requests, IndexedEntity.INDEX, 0, HOST_1, "2",
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
						+ "'prefix_customBridgeOnClass': {"
							+ "'text': 'some more text (3)',"
							+ "'date': '2017-11-03'"
						+ "},"
						+ "'prefix_myLocalDateField': '2017-11-03',"
						+ "'prefix_myTextField': 'some more text (3)'"
					+ "},"
					+ "'myTextField': 'some more text (2)'"
				+ "}" );
		assertIndexDocumentRequest( requests, IndexedEntity.INDEX, 1, HOST_1, "3",
				"{"
					+ "'customBridgeOnClass': {"
						+ "'text': 'some more text (3)',"
						+ "'date': '2017-11-03'"
					+ "},"
					+ "'myLocalDateField': '2017-11-03',"
					+ "'myTextField': 'some more text (3)'"
				+ "}" );
		assertIndexDocumentRequest( requests, OtherIndexedEntity.INDEX, 0, HOST_2, "4",
				"{"
					+ "'numeric': 404,"
					+ "'numericAsString': '404'"
				+ "}" );
	}

	@Test
	public void search() throws JSONException {
		OrmUtils.withinSession( sessionFactory, session -> {
			FullTextSession ftSession = Search.getFullTextSession( session );
			FullTextQuery<ParentIndexedEntity> query = ftSession.search(
							Arrays.asList( IndexedEntity.class, YetAnotherIndexedEntity.class )
					)
					.query()
					.asEntities()
					.predicate().bool( b -> {
						b.must().match()
								.onField( "myTextField" ).boostedTo( 1.5f )
								.orField( "embedded.prefix_myTextField" ).boostedTo( 0.9f )
								.matching( "foo" );
						b.should().range()
								.onField( "myLocalDateField" )
								.from( LocalDate.of( 2017, 10, 01 ), RangeBoundInclusion.EXCLUDED )
								.to( LocalDate.of( 2017, 11, 01 ) );
						/*
						 * Alternative syntax taking advantage of lambdas,
						 * allowing to introduce if/else statements in the query building code
						 * and removing the need to call .end() on nested clause contexts.
						 */
						b.should( c -> {
							if ( /* put some condition here, for example based on user input */ true ) {
								c.range()
										.onField( "numeric" )
										.above( 12 );
							}
						} );
					} )
					.sort().byField( "numeric" ).desc()
							.then().byField( "myLocalDateField" )
							.then().byScore()
							.end()
					.build();
			query.setFirstResult( 3 );
			query.setMaxResults( 2 );

			StubElasticsearchClient.pushStubResponse(
					"{"
						+ "'hits': {"
							+ "'total': 6,"
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
			List<ParentIndexedEntity> result = query.list();
			Assertions.assertThat( result ).hasSize( 2 )
					.containsExactly(
							session.get( IndexedEntity.class, 0 ),
							session.get( YetAnotherIndexedEntity.class, 1 )
					);
			// TODO getResultSize
		} );

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();
		assertRequest( requests, Arrays.asList( IndexedEntity.INDEX, YetAnotherIndexedEntity.INDEX ), 0,
				HOST_1, "POST", "/_search",
				c -> {
					c.accept( "from", "3" );
					c.accept( "size", "2" );
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
													+ "'query': 'foo',"
													+ "'boost': 1.5"
												+ "}"
											+ "}"
										+ "},"
										+ "{"
											+ "'match': {"
												+ "'embedded.prefix_myTextField': {"
													+ "'query': 'foo',"
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
					+ "},"
					+ "'sort': ["
						+ "{"
							+ "'numeric': {"
								+ "'order': 'desc'"
							+ "}"
						+ "},"
						+ "'myLocalDateField',"
						+ "'_score'"
					+ "]"
				+ "}" );
	}

	@Test
	public void search_separatePredicate() throws JSONException {
		OrmUtils.withinSession( sessionFactory, session -> {
			FullTextSession ftSession = Search.getFullTextSession( session );
			FullTextSearchTarget<ParentIndexedEntity> target = ftSession.search(
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
									.orField( "embedded.prefix_myTextField" ).boostedTo( 0.9f )
									.matching( "foo" )
							.should( nestedPredicate )
							.mustNot().predicate( otherNestedPredicate )
					.end();
			SearchSort nestedSort = target.sort().byField( "numeric" ).desc().end();
			SearchSort otherNestedSort = target.sort().byScore().end();
			SearchSort sort = target.sort()
					.by( nestedSort )
					.then().byField( "myLocalDateField" )
					.then().by( otherNestedSort )
					.end();
			FullTextQuery<ParentIndexedEntity> query = target.query()
					.asEntities()
					.predicate( predicate )
					.sort( sort )
					.build();

			StubElasticsearchClient.pushStubResponse(
					"{"
						+ "'hits': {"
							+ "'total': 2,"
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
			query.list();
		} );

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();
		assertRequest( requests, Arrays.asList( IndexedEntity.INDEX, YetAnotherIndexedEntity.INDEX ), 0,
				HOST_1, "POST", "/_search",
				null,
				"{"
					+ "'query': {"
						+ "'bool': {"
							+ "'must': {"
								+ "'bool': {"
									+ "'should': ["
										+ "{"
											+ "'match': {"
												+ "'myTextField': {"
													+ "'query': 'foo',"
													+ "'boost': 1.5"
												+ "}"
											+ "}"
										+ "},"
										+ "{"
											+ "'match': {"
												+ "'embedded.prefix_myTextField': {"
													+ "'query': 'foo',"
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
					+ "},"
					+ "'sort': ["
						+ "{"
							+ "'numeric': {"
								+ "'order': 'desc'"
							+ "}"
						+ "},"
						+ "'myLocalDateField',"
						+ "'_score'"
					+ "]"
				+ "}" );
	}

	@Test
	public void search_projection() throws JSONException {
		OrmUtils.withinSession( sessionFactory, session -> {
			FullTextSession ftSession = Search.getFullTextSession( session );
			FullTextQuery<List<?>> query = ftSession.search(
							Arrays.asList( IndexedEntity.class, YetAnotherIndexedEntity.class )
					)
					.query()
					.asProjections(
							"myTextField",
							ProjectionConstants.REFERENCE,
							"myLocalDateField",
							ProjectionConstants.DOCUMENT_REFERENCE,
							ProjectionConstants.OBJECT,
							"customBridgeOnClass.text"
					)
					.predicate().all().end()
					.build();

			StubElasticsearchClient.pushStubResponse(
					"{"
						+ "'hits': {"
							+ "'total': 2,"
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
			List<List<?>> result = query.list();
			Assertions.assertThat( result ).hasSize( 2 )
					.containsExactly(
							Arrays.asList(
									"text1",
									new PojoReferenceImpl( IndexedEntity.class, 0 ),
									LocalDate.of( 2017, 11, 1 ),
									new ElasticsearchDocumentReference( IndexedEntity.INDEX, "0" ),
									session.get( IndexedEntity.class, 0 ),
									"text2"
							),
							Arrays.asList(
									null,
									new PojoReferenceImpl( YetAnotherIndexedEntity.class, 1 ),
									LocalDate.of( 2017, 11, 2 ),
									new ElasticsearchDocumentReference( YetAnotherIndexedEntity.INDEX, "1" ),
									session.get( YetAnotherIndexedEntity.class, 1 ),
									null
							)
					);
		} );

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();
		assertRequest( requests, Arrays.asList( IndexedEntity.INDEX, YetAnotherIndexedEntity.INDEX ), 0,
				HOST_1, "POST", "/_search",
				null,
				"{"
					+ "'query': {"
						+ "'match_all': {}"
					+ "},"
					+ "'_source': ["
						+ "'myTextField',"
						+ "'myLocalDateField',"
						+ "'customBridgeOnClass.text'"
					+ "]"
				+ "}" );
	}

	private class MyMappingContributor implements HibernateOrmSearchMappingContributor {
		@Override
		public void contribute(HibernateOrmMappingContributor contributor) {
			ProgrammaticMappingDefinition mapping = contributor.programmaticMapping();
			mapping.type( IndexedEntity.class )
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
									.prefix( "embedded.prefix_" )
									.maxDepth( 1 )
									.includePaths( "embedded.prefix_customBridgeOnClass.text" );

			ProgrammaticMappingDefinition secondMapping = contributor.programmaticMapping();
			secondMapping.type( ParentIndexedEntity.class )
					.property( "localDate" )
							.field()
									.name( "myLocalDateField" )
					.property( "embedded" )
							.bridge(
									new MyBridgeBuilder()
									.objectName( "customBridgeOnProperty" )
							);
			secondMapping.type( OtherIndexedEntity.class )
					.indexed( OtherIndexedEntity.INDEX )
					.property( "id" )
							.documentId().identifierBridge( DefaultIntegerIdentifierBridge.class )
					.property( "numeric" )
							.field()
							.field().name( "numericAsString" ).functionBridge( IntegerAsStringFunctionBridge.class );
			secondMapping.type( YetAnotherIndexedEntity.class )
					.indexed( YetAnotherIndexedEntity.INDEX )
					.property( "id" )
							.documentId()
					.property( "numeric" )
							.field();
		}
	}

	@MappedSuperclass
	public static class ParentIndexedEntity {

		private LocalDate localDate;

		@ManyToOne
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

	@Entity
	@Table(name = "indexed")
	public static class IndexedEntity extends ParentIndexedEntity {

		public static final String INDEX = "IndexedEntity";

		@Id
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

	@Entity
	@Table(name = "other")
	public static class OtherIndexedEntity {

		public static final String INDEX = "OtherIndexedEntity";

		@Id
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

	@Entity
	@Table(name = "yetanother")
	public static class YetAnotherIndexedEntity extends ParentIndexedEntity {

		public static final String INDEX = "YetAnotherIndexedEntity";

		@Id
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
		private IndexObjectFieldAccessor objectFieldAccessor;
		private IndexFieldAccessor<String> textFieldAccessor;
		private IndexFieldAccessor<LocalDate> localDateFieldAccessor;

		MyBridgeImpl(String objectName) {
			this.objectName = objectName;
		}

		@Override
		public void bind(IndexSchemaElement indexSchemaElement, PojoModelElement bridgedPojoModelElement,
				SearchModel searchModel) {
			sourceAccessor = bridgedPojoModelElement.createAccessor( IndexedEntity.class );
			IndexSchemaObjectField objectField = indexSchemaElement.objectField( objectName );
			objectFieldAccessor = objectField.createAccessor();
			textFieldAccessor = objectField.field( "text" ).asString().createAccessor();
			localDateFieldAccessor = objectField.field( "date" ).asLocalDate().createAccessor();
		}

		@Override
		public void write(DocumentElement target, PojoElement source) {
			IndexedEntity sourceValue = sourceAccessor.read( source );
			if ( sourceValue != null ) {
				DocumentElement object = objectFieldAccessor.add( target );
				textFieldAccessor.write( object, sourceValue.getText() );
				localDateFieldAccessor.write( object, sourceValue.getLocalDate() );
			}
		}

	}
}

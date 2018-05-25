/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm;

import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.Type;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.SearchOrmSettings;
import org.hibernate.search.mapper.orm.hibernate.FullTextQuery;
import org.hibernate.search.mapper.orm.hibernate.FullTextSession;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingInitiator;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingContributor;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultIntegerIdentifierBridge;
import org.hibernate.search.mapper.pojo.extractor.builtin.MapKeyExtractor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingDefinition;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoReferenceImpl;
import org.hibernate.search.integrationtest.mapper.orm.bridge.CustomPropertyBridge;
import org.hibernate.search.integrationtest.mapper.orm.bridge.CustomTypeBridge;
import org.hibernate.search.integrationtest.mapper.orm.bridge.IntegerAsStringValueBridge;
import org.hibernate.search.integrationtest.mapper.orm.bridge.OptionalIntAsStringValueBridge;
import org.hibernate.search.integrationtest.mapper.orm.usertype.OptionalIntUserType;
import org.hibernate.search.integrationtest.mapper.orm.usertype.OptionalStringUserType;
import org.hibernate.search.engine.search.ProjectionConstants;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackendFactory;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.rule.StaticCounters;
import org.hibernate.service.ServiceRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;

/**
 * @author Yoann Rodiere
 */
public class OrmProgrammaticMappingIT {

	private static final String PREFIX = SearchOrmSettings.PREFIX;

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public StaticCounters counters = new StaticCounters();

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
				.applySetting( PREFIX + "backend.stubBackend.type", StubBackendFactory.class.getName() )
				.applySetting( PREFIX + "index.default.backend", "stubBackend" )
				.applySetting( SearchOrmSettings.MAPPING_CONTRIBUTOR, new MyMappingContributor() );

		ServiceRegistry serviceRegistry = registryBuilder.build();

		MetadataSources ms = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( IndexedEntity.class )
				.addAnnotatedClass( ParentIndexedEntity.class )
				.addAnnotatedClass( OtherIndexedEntity.class )
				.addAnnotatedClass( YetAnotherIndexedEntity.class );

		Metadata metadata = ms.buildMetadata();

		final SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();

		backendMock.expectSchema( OtherIndexedEntity.INDEX, b -> b
				.field( "numeric", Integer.class )
				.field( "numericAsString", String.class )
		);
		backendMock.expectSchema( YetAnotherIndexedEntity.INDEX, b -> b
				.objectField( "customBridgeOnProperty", b2 -> b2
						.field( "date", LocalDate.class )
						.field( "text", String.class )
				)
				.field( "myLocalDateField", LocalDate.class )
				.field( "numeric", Integer.class )
				.field( "optionalText", String.class )
				.field( "optionalInt", Integer.class )
				.field( "optionalIntAsString", String.class )
				.field( "numericArray", Integer.class )
				.objectField( "embeddedList", b2 -> b2
						.objectField( "otherPrefix_embedded", b3 -> b3
								.objectField( "prefix_customBridgeOnClass", b4 -> b4
										.field( "text", String.class )
								)
						)
				)
				.field( "embeddedMapKeys", String.class )
				.objectField( "embeddedMap", b2 -> b2
						.objectField( "embedded", b3 -> b3
								.field( "prefix_myLocalDateField", LocalDate.class )
						)
				)
		);
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "customBridgeOnClass", b2 -> b2
						.field( "date", LocalDate.class )
						.field( "text", String.class )
				)
				.objectField( "customBridgeOnProperty", b2 -> b2
						.field( "date", LocalDate.class )
						.field( "text", String.class )
				)
				.objectField( "embedded", b2 -> b2
						.objectField( "prefix_customBridgeOnClass", b3 -> b3
								.field( "date", LocalDate.class )
								.field( "text", String.class )
						)
						.objectField( "prefix_customBridgeOnProperty", b3 -> b3
								.field( "date", LocalDate.class )
								.field( "text", String.class )
						)
						.objectField( "prefix_embedded", b3 -> b3
								.objectField( "prefix_customBridgeOnClass", b4 -> b4
										.field( "text", String.class )
								)
						)
						.field( "prefix_myLocalDateField", LocalDate.class )
						.field( "prefix_myTextField", String.class )
				)
				.field( "myTextField", String.class )
				.field( "myLocalDateField", LocalDate.class )
		);

		sessionFactory = sfb.build();
		backendMock.verifyExpectationsMet();
	}

	@After
	public void cleanup() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	@Test
	public void index() {
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
			YetAnotherIndexedEntity entity5 = new YetAnotherIndexedEntity();
			entity5.setId( 5 );
			entity5.setNumeric( 405 );
			entity5.setOptionalText( Optional.of( "some more text (5)" ) );
			entity5.setOptionalInt( OptionalInt.of( 42 ) );
			entity5.setNumericArray( new Integer[] { 1, 2, 3 } );
			IndexedEntity entity6 = new IndexedEntity();
			entity6.setId( 6 );
			entity6.setText( "some more text (6)" );
			entity6.setLocalDate( LocalDate.of( 2017, 11, 6 ) );

			entity1.setEmbedded( entity2 );
			entity2.getEmbeddingAsSingleFromIndexed().add( entity1 );
			entity2.setEmbedded( entity3 );
			entity3.getEmbeddingAsSingleFromIndexed().add( entity2 );
			entity3.setEmbedded( entity2 );
			entity2.getEmbeddingAsSingleFromIndexed().add( entity3 );
			entity5.setEmbeddedList( Arrays.asList( entity2, entity3, entity6 ) );
			entity2.getEmbeddingAsListFromYetAnotherIndexed().add( entity5 );
			entity3.getEmbeddingAsListFromYetAnotherIndexed().add( entity5 );
			entity6.getEmbeddingAsListFromYetAnotherIndexed().add( entity5 );
			Map<String, IndexedEntity> embeddedMap = new LinkedHashMap<>();
			embeddedMap.put( "entity3", entity3 );
			embeddedMap.put( "entity2", entity2 );
			entity5.setEmbeddedMap( embeddedMap );
			entity3.getEmbeddingAsMapFromYetAnotherIndexed().add( entity5 );
			entity2.getEmbeddingAsMapFromYetAnotherIndexed().add( entity5 );

			session.persist( entity1 );
			session.persist( entity2 );
			session.persist( entity4 );
			session.delete( entity1 );
			session.persist( entity3 );
			session.persist( entity5 );
			session.persist( entity6 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "2", b -> b
							.field( "myLocalDateField", entity2.getLocalDate() )
							.field( "myTextField", entity2.getText() )
							.objectField( "customBridgeOnClass", b2 -> b2
									.field( "text", entity2.getText() )
									.field( "date", entity2.getLocalDate() )
							)
							.objectField( "customBridgeOnProperty", b2 -> b2
									.field( "text", entity2.getEmbedded().getText() )
									.field( "date", entity2.getEmbedded().getLocalDate() )
							)
							.objectField( "embedded", b2 -> b2
									.field( "prefix_myTextField", entity2.getEmbedded().getText() )
									.field( "prefix_myLocalDateField", entity2.getEmbedded().getLocalDate() )
									.objectField( "prefix_customBridgeOnClass", b3 -> b3
											.field( "text", entity2.getEmbedded().getText() )
											.field( "date", entity2.getEmbedded().getLocalDate() )
									)
									.objectField( "prefix_customBridgeOnProperty", b3 -> b3
											.field( "text", entity2.getEmbedded().getEmbedded().getText() )
											.field( "date", entity2.getEmbedded().getEmbedded().getLocalDate() )
									)
									.objectField( "prefix_embedded", b3 -> b3
											.objectField( "prefix_customBridgeOnClass", b4 -> b4
													.field( "text", entity2.getEmbedded().getEmbedded().getText() )
											)
									)
							)
					)
					.add( "3", b -> b
							.field( "myLocalDateField", entity3.getLocalDate() )
							.field( "myTextField", entity3.getText() )
							.objectField( "customBridgeOnClass", b2 -> b2
									.field( "text", entity3.getText() )
									.field( "date", entity3.getLocalDate() )
							)
							.objectField( "customBridgeOnProperty", b2 -> b2
									.field( "text", entity3.getEmbedded().getText() )
									.field( "date", entity3.getEmbedded().getLocalDate() )
							)
							.objectField( "embedded", b2 -> b2
									.field( "prefix_myTextField", entity3.getEmbedded().getText() )
									.field( "prefix_myLocalDateField", entity3.getEmbedded().getLocalDate() )
									.objectField( "prefix_customBridgeOnClass", b3 -> b3
											.field( "text", entity3.getEmbedded().getText() )
											.field( "date", entity3.getEmbedded().getLocalDate() )
									)
									.objectField( "prefix_customBridgeOnProperty", b3 -> b3
											.field( "text", entity3.getEmbedded().getEmbedded().getText() )
											.field( "date", entity3.getEmbedded().getEmbedded().getLocalDate() )
									)
									.objectField( "prefix_embedded", b3 -> b3
											.objectField( "prefix_customBridgeOnClass", b4 -> b4
													.field( "text", entity3.getEmbedded().getEmbedded().getText() )
											)
									)
							)
					)
					.add( "6", b -> b
							.field( "myLocalDateField", entity6.getLocalDate() )
							.field( "myTextField", entity6.getText() )
							.objectField( "customBridgeOnClass", b2 -> b2
									.field( "text", entity6.getText() )
									.field( "date", entity6.getLocalDate() )
							)
					)
					.preparedThenExecuted();
			backendMock.expectWorks( OtherIndexedEntity.INDEX )
					.add( "4", b -> b
							.field( "numeric", entity4.getNumeric() )
							.field( "numericAsString", String.valueOf( entity4.getNumeric() ) )
					)
					.preparedThenExecuted();
			backendMock.expectWorks( YetAnotherIndexedEntity.INDEX )
					.add( "5", b -> b
							.field( "myLocalDateField", entity5.getLocalDate() )
							.field( "numeric", entity5.getNumeric() )
							.field( "optionalText", entity5.getOptionalText().get() )
							.field( "optionalInt", entity5.getOptionalInt().getAsInt() )
							.field( "optionalIntAsString", String.valueOf( entity5.getOptionalInt().getAsInt() ) )
							.field( "numericArray", entity5.getNumericArray()[0] )
							.field( "numericArray", entity5.getNumericArray()[1] )
							.field( "numericArray", entity5.getNumericArray()[2] )
							.objectField( "embeddedList", b2 -> b2
									.objectField( "otherPrefix_embedded", b3 -> b3
											.objectField( "prefix_customBridgeOnClass", b4 -> b4
													.field( "text", entity2.getEmbedded().getText() )
											)
									)
							)
							.objectField( "embeddedList", b2 -> b2
									.objectField( "otherPrefix_embedded", b3 -> b3
											.objectField( "prefix_customBridgeOnClass", b4 -> b4
													.field( "text", entity3.getEmbedded().getText() )
											)
									)
							)
							.objectField( "embeddedList", b2 -> { } )
							.objectField( "embeddedMap", b2 -> b2
									.objectField( "embedded", b3 -> b3
											.field( "prefix_myLocalDateField", entity3.getEmbedded().getLocalDate() )
									)
							)
							.field( "embeddedMapKeys", "entity3", "entity2" )
							.objectField( "embeddedMap", b2 -> b2
									.objectField( "embedded", b3 -> b3
											.field( "prefix_myLocalDateField", entity2.getEmbedded().getLocalDate() )
									)
							)
					)
					.preparedThenExecuted();
		} );
	}

	@Test
	public void search() {
		OrmUtils.withinSession( sessionFactory, session -> {
			FullTextSession ftSession = Search.getFullTextSession( session );
			FullTextQuery<ParentIndexedEntity> query = ftSession.search(
							Arrays.asList( IndexedEntity.class, YetAnotherIndexedEntity.class )
					)
					.query()
					.asEntities()
					.predicate().matchAll().end()
					.build();
			query.setFirstResult( 3 );
			query.setMaxResults( 2 );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.INDEX, YetAnotherIndexedEntity.INDEX ),
					b -> b
							.firstResultIndex( 3L )
							.maxResultsCount( 2L ),
					StubSearchWorkBehavior.of(
							6L,
							c -> c.collectForLoading( reference( IndexedEntity.INDEX, "0" ) ),
							c -> c.collectForLoading( reference( YetAnotherIndexedEntity.INDEX, "1" ) )
					)
			);

			List<ParentIndexedEntity> result = query.list();
			backendMock.verifyExpectationsMet();
			Assertions.assertThat( result )
					.containsExactly(
							session.get( IndexedEntity.class, 0 ),
							session.get( YetAnotherIndexedEntity.class, 1 )
					);
			// TODO getResultSize
		} );
	}

	@Test
	public void search_projection() {
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
					.predicate().matchAll().end()
					.build();
			query.setFirstResult( 3 );
			query.setMaxResults( 2 );

			backendMock.expectSearchProjections(
					Arrays.asList( IndexedEntity.INDEX, YetAnotherIndexedEntity.INDEX ),
					b -> b
							.firstResultIndex( 3L )
							.maxResultsCount( 2L ),
					StubSearchWorkBehavior.of(
							2L,
							c -> {
								c.collectProjection( "text1" );
								c.collectReference( reference( IndexedEntity.INDEX, "0" ) );
								c.collectProjection( LocalDate.of( 2017, 11, 1 ) );
								c.collectProjection( reference( IndexedEntity.INDEX, "0" ) );
								c.collectForLoading( reference( IndexedEntity.INDEX, "0" ) );
								c.collectProjection( "text2" );
							},
							c -> {
								c.collectProjection( null );
								c.collectReference( reference( YetAnotherIndexedEntity.INDEX, "1" ) );
								c.collectProjection( LocalDate.of( 2017, 11, 2 ) );
								c.collectProjection( reference( YetAnotherIndexedEntity.INDEX, "1" ) );
								c.collectForLoading( reference( YetAnotherIndexedEntity.INDEX, "1" ) );
								c.collectProjection( null );
							}
					)
			);

			List<List<?>> result = query.list();
			backendMock.verifyExpectationsMet();
			Assertions.assertThat( result )
					.containsExactly(
							Arrays.asList(
									"text1",
									new PojoReferenceImpl( IndexedEntity.class, 0 ),
									LocalDate.of( 2017, 11, 1 ),
									reference( IndexedEntity.INDEX, "0" ),
									session.get( IndexedEntity.class, 0 ),
									"text2"
							),
							Arrays.asList(
									null,
									new PojoReferenceImpl( YetAnotherIndexedEntity.class, 1 ),
									LocalDate.of( 2017, 11, 2 ),
									reference( YetAnotherIndexedEntity.INDEX, "1" ),
									session.get( YetAnotherIndexedEntity.class, 1 ),
									null
							)
					);
		} );
	}

	private class MyMappingContributor implements HibernateOrmSearchMappingContributor {
		@Override
		public void contribute(HibernateOrmMappingInitiator initiator) {
			ProgrammaticMappingDefinition mapping = initiator.programmaticMapping();
			mapping.type( IndexedEntity.class )
					.indexed( IndexedEntity.INDEX )
					.bridge(
							new CustomTypeBridge.Builder()
							.objectName( "customBridgeOnClass" )
					)
					.property( "id" )
							.documentId()
					.property( "text" )
							.field( "myTextField" )
					.property( "embedded" )
							.indexedEmbedded()
									.prefix( "embedded.prefix_" )
									.maxDepth( 1 )
									.includePaths( "customBridgeOnClass.text", "embedded.prefix_customBridgeOnClass.text" );

			ProgrammaticMappingDefinition secondMapping = initiator.programmaticMapping();
			secondMapping.type( ParentIndexedEntity.class )
					.property( "localDate" )
							.field( "myLocalDateField" )
					.property( "embedded" )
							.bridge(
									new CustomPropertyBridge.Builder()
									.objectName( "customBridgeOnProperty" )
							);
			secondMapping.type( OtherIndexedEntity.class )
					.indexed( OtherIndexedEntity.INDEX )
					.property( "id" )
							.documentId().identifierBridge( DefaultIntegerIdentifierBridge.class )
					.property( "numeric" )
							.field()
							.field( "numericAsString" ).valueBridge( IntegerAsStringValueBridge.class );
			secondMapping.type( YetAnotherIndexedEntity.class )
					.indexed( YetAnotherIndexedEntity.INDEX )
					.property( "id" )
							.documentId()
					.property( "numeric" )
							.field()
					.property( "optionalText" )
							.field()
					.property( "optionalInt" )
							.field()
							.field( "optionalIntAsString" )
									.valueBridge( OptionalIntAsStringValueBridge.class )
									.withoutExtractors()
					.property( "numericArray" )
							.field()
					.property( "embeddedList" )
							.indexedEmbedded()
									.prefix( "embeddedList.otherPrefix_" )
									.includePaths( "embedded.prefix_customBridgeOnClass.text" )
					.property( "embeddedMap" )
							.field( "embeddedMapKeys" ).withExtractor( MapKeyExtractor.class )
							.indexedEmbedded().includePaths( "embedded.prefix_myLocalDateField" );
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

		@OneToMany(mappedBy = "embedded")
		private List<IndexedEntity> embeddingAsSingleFromIndexed = new ArrayList<>();

		@OneToMany(mappedBy = "embedded")
		private List<YetAnotherIndexedEntity> embeddingAsSingleFromYetAnotherIndexed = new ArrayList<>();

		@ManyToMany(mappedBy = "embeddedList")
		private List<YetAnotherIndexedEntity> embeddingAsListFromYetAnotherIndexed = new ArrayList<>();

		@ManyToMany(mappedBy = "embeddedMap")
		private List<YetAnotherIndexedEntity> embeddingAsMapFromYetAnotherIndexed = new ArrayList<>();


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

		public List<IndexedEntity> getEmbeddingAsSingleFromIndexed() {
			return embeddingAsSingleFromIndexed;
		}

		public List<YetAnotherIndexedEntity> getEmbeddingAsSingleFromYetAnotherIndexed() {
			return embeddingAsSingleFromYetAnotherIndexed;
		}

		public List<YetAnotherIndexedEntity> getEmbeddingAsListFromYetAnotherIndexed() {
			return embeddingAsListFromYetAnotherIndexed;
		}

		public List<YetAnotherIndexedEntity> getEmbeddingAsMapFromYetAnotherIndexed() {
			return embeddingAsMapFromYetAnotherIndexed;
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

		private String optionalText;

		private Integer optionalInt;

		private Integer[] numericArray;

		@ManyToMany
		@JoinTable(name = "yetanother_indexed_list")
		private List<IndexedEntity> embeddedList;

		@ManyToMany
		@JoinTable(name = "yetanother_indexed_map")
		private Map<String, IndexedEntity> embeddedMap;

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

		@Access( AccessType.PROPERTY )
		@Type( type = OptionalStringUserType.NAME )
		public Optional<String> getOptionalText() {
			return Optional.ofNullable( optionalText );
		}

		public void setOptionalText(Optional<String> text) {
			this.optionalText = text.orElse( null );
		}

		@Access( AccessType.PROPERTY )
		@Type( type = OptionalIntUserType.NAME )
		public OptionalInt getOptionalInt() {
			return optionalInt == null ? OptionalInt.empty() : OptionalInt.of( optionalInt );
		}

		public void setOptionalInt(OptionalInt value) {
			this.optionalInt = value.isPresent() ? value.getAsInt() : null;
		}

		public Integer[] getNumericArray() {
			return numericArray;
		}

		public void setNumericArray(Integer[] numericArray) {
			this.numericArray = numericArray;
		}

		public List<IndexedEntity> getEmbeddedList() {
			return embeddedList;
		}

		public void setEmbeddedList(List<IndexedEntity> embeddedList) {
			this.embeddedList = embeddedList;
		}

		public Map<String, IndexedEntity> getEmbeddedMap() {
			return embeddedMap;
		}

		public void setEmbeddedMap(Map<String, IndexedEntity> embeddedMap) {
			this.embeddedMap = embeddedMap;
		}
	}

}

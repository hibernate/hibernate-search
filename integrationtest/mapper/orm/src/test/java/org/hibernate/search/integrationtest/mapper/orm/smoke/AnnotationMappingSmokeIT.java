/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.smoke;

import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.assertj.core.api.Assertions;
import org.hibernate.SessionFactory;
import org.hibernate.search.integrationtest.mapper.orm.smoke.bridge.CustomPropertyBridgeAnnotation;
import org.hibernate.search.integrationtest.mapper.orm.smoke.bridge.CustomTypeBridgeAnnotation;
import org.hibernate.search.integrationtest.mapper.orm.smoke.bridge.IntegerAsStringValueBridge;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultIntegerIdentifierBridge;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ContainerExtractorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ValueBridgeRef;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.rule.StaticCounters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
public class AnnotationMappingSmokeIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = new OrmSetupHelper();

	@Rule
	public StaticCounters counters = new StaticCounters();

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
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

		sessionFactory = ormSetupHelper.withBackendMock( backendMock )
				.setup(
						IndexedEntity.class,
						ParentIndexedEntity.class,
						OtherIndexedEntity.class,
						YetAnotherIndexedEntity.class
				);
		backendMock.verifyExpectationsMet();
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
			SearchSession searchSession = Search.getSearchSession( session );
			SearchQuery<ParentIndexedEntity> query = searchSession.search(
							Arrays.asList( IndexedEntity.class, YetAnotherIndexedEntity.class )
					)
					.asEntity()
					.predicate( f -> f.matchAll() )
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
							reference( IndexedEntity.INDEX, "0" ),
							reference( YetAnotherIndexedEntity.INDEX, "1" )
					)
			);

			List<ParentIndexedEntity> result = query.getResultList();
			backendMock.verifyExpectationsMet();
			Assertions.assertThat( result )
					.containsExactly(
							session.get( IndexedEntity.class, 0 ),
							session.get( YetAnotherIndexedEntity.class, 1 )
					);

			backendMock.expectCount( Arrays.asList( IndexedEntity.INDEX, YetAnotherIndexedEntity.INDEX ), 6L );

			long resultSize = query.getResultSize();
			backendMock.verifyExpectationsMet();
			Assertions.assertThat( resultSize ).isEqualTo( 6L );
		} );
	}

	@MappedSuperclass
	public static class ParentIndexedEntity {

		@GenericField(name = "myLocalDateField")
		private LocalDate localDate;

		@ManyToOne
		@CustomPropertyBridgeAnnotation(objectName = "customBridgeOnProperty")
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
	@Indexed(index = IndexedEntity.INDEX)
	@CustomTypeBridgeAnnotation(objectName = "customBridgeOnClass")
	public static class IndexedEntity extends ParentIndexedEntity {

		public static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@GenericField(name = "myTextField")
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

		@Override
		@IndexedEmbedded(prefix = "embedded.prefix_", maxDepth = 1,
				includePaths = { "customBridgeOnClass.text", "embedded.prefix_customBridgeOnClass.text" })
		public IndexedEntity getEmbedded() {
			return super.getEmbedded();
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
	@Indexed(index = OtherIndexedEntity.INDEX)
	public static class OtherIndexedEntity {

		public static final String INDEX = "OtherIndexedEntity";

		@Id
		private Integer id;

		@Column(name = "numeric_column")
		@GenericField(name = "numericAsString", valueBridge = @ValueBridgeRef(type = IntegerAsStringValueBridge.class))
		private Integer numeric;

		@DocumentId(identifierBridge = @IdentifierBridgeRef(type = DefaultIntegerIdentifierBridge.class))
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@GenericField
		public Integer getNumeric() {
			return numeric;
		}

		public void setNumeric(Integer numeric) {
			this.numeric = numeric;
		}

	}

	@Entity
	@Table(name = "yetanother")
	@Indexed(index = YetAnotherIndexedEntity.INDEX)
	public static class YetAnotherIndexedEntity extends ParentIndexedEntity {

		public static final String INDEX = "YetAnotherIndexedEntity";

		@Id
		private Integer id;

		@Column(name = "numeric_column")
		@GenericField
		private Integer numeric;

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

		@IndexedEmbedded(prefix = "embeddedList.otherPrefix_", includePaths = "embedded.prefix_customBridgeOnClass.text")
		public List<IndexedEntity> getEmbeddedList() {
			return embeddedList;
		}

		public void setEmbeddedList(List<IndexedEntity> embeddedList) {
			this.embeddedList = embeddedList;
		}

		@IndexedEmbedded(includePaths = "embedded.prefix_myLocalDateField")
		@GenericField(
				name = "embeddedMapKeys",
				extractors = @ContainerExtractorRef(BuiltinContainerExtractor.MAP_KEY)
		)
		public Map<String, IndexedEntity> getEmbeddedMap() {
			return embeddedMap;
		}

		public void setEmbeddedMap(Map<String, IndexedEntity> embeddedMap) {
			this.embeddedMap = embeddedMap;
		}
	}

}

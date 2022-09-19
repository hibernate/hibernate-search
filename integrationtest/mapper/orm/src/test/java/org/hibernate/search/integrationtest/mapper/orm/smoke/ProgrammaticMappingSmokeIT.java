/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.mapper.orm.smoke.bridge.CustomPropertyBridge;
import org.hibernate.search.integrationtest.mapper.orm.smoke.bridge.CustomTypeBridge;
import org.hibernate.search.integrationtest.mapper.orm.smoke.bridge.IntegerAsStringValueBridge;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

public class ProgrammaticMappingSmokeIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectSchema( OtherIndexedEntity.NAME, b -> b
				.field( "numeric", Integer.class )
				.field( "numericAsString", String.class )
		);
		backendMock.expectSchema( YetAnotherIndexedEntity.NAME, b -> b
				.objectField( "customBridgeOnProperty", b2 -> b2
						.field( "date", LocalDate.class )
						.field( "text", String.class )
				)
				.field( "myLocalDateField", LocalDate.class )
				.field( "numeric", Integer.class )
				.objectField( "myEmbeddedList", b2 -> b2
						.multiValued( true )
						.objectField( "myEmbedded", b3 -> b3
								.objectField( "customBridgeOnClass", b4 -> b4
										.field( "text", String.class )
								)
						)
				)
				.field( "embeddedMapKeys", String.class, b2 -> b2.multiValued( true ) )
				.objectField( "embeddedMap", b2 -> b2
						.multiValued( true )
						.objectField( "myEmbedded", b3 -> b3
								.field( "myLocalDateField", LocalDate.class )
						)
				)
		);
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.objectField( "customBridgeOnClass", b2 -> b2
						.field( "date", LocalDate.class )
						.field( "text", String.class )
				)
				.objectField( "customBridgeOnProperty", b2 -> b2
						.field( "date", LocalDate.class )
						.field( "text", String.class )
				)
				.objectField( "myEmbedded", b2 -> b2
						.objectField( "customBridgeOnClass", b3 -> b3
								.field( "date", LocalDate.class )
								.field( "text", String.class )
						)
						.objectField( "customBridgeOnProperty", b3 -> b3
								.field( "date", LocalDate.class )
								.field( "text", String.class )
						)
						.objectField( "myEmbedded", b3 -> b3
								.objectField( "customBridgeOnClass", b4 -> b4
										.field( "text", String.class )
								)
						)
						.field( "myLocalDateField", LocalDate.class )
						.field( "myTextField", String.class )
				)
				.field( "myTextField", String.class )
				.field( "myLocalDateField", LocalDate.class )
		);

		setupContext.withProperty( HibernateOrmMapperSettings.MAPPING_CONFIGURER, new MyMappingConfigurer() )
				.withAnnotatedTypes(
						IndexedEntity.class,
						ParentIndexedEntity.class,
						OtherIndexedEntity.class,
						YetAnotherIndexedEntity.class
				);
	}

	@Test
	public void index() {
		setupHolder.runInTransaction( session -> {
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
			session.remove( entity1 );
			session.persist( entity3 );
			session.persist( entity5 );
			session.persist( entity6 );

			backendMock.expectWorks( IndexedEntity.NAME )
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
							.objectField( "myEmbedded", b2 -> b2
									.field( "myTextField", entity2.getEmbedded().getText() )
									.field( "myLocalDateField", entity2.getEmbedded().getLocalDate() )
									.objectField( "customBridgeOnClass", b3 -> b3
											.field( "text", entity2.getEmbedded().getText() )
											.field( "date", entity2.getEmbedded().getLocalDate() )
									)
									.objectField( "customBridgeOnProperty", b3 -> b3
											.field( "text", entity2.getEmbedded().getEmbedded().getText() )
											.field( "date", entity2.getEmbedded().getEmbedded().getLocalDate() )
									)
									.objectField( "myEmbedded", b3 -> b3
											.objectField( "customBridgeOnClass", b4 -> b4
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
							.objectField( "myEmbedded", b2 -> b2
									.field( "myTextField", entity3.getEmbedded().getText() )
									.field( "myLocalDateField", entity3.getEmbedded().getLocalDate() )
									.objectField( "customBridgeOnClass", b3 -> b3
											.field( "text", entity3.getEmbedded().getText() )
											.field( "date", entity3.getEmbedded().getLocalDate() )
									)
									.objectField( "customBridgeOnProperty", b3 -> b3
											.field( "text", entity3.getEmbedded().getEmbedded().getText() )
											.field( "date", entity3.getEmbedded().getEmbedded().getLocalDate() )
									)
									.objectField( "myEmbedded", b3 -> b3
											.objectField( "customBridgeOnClass", b4 -> b4
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
					);
			backendMock.expectWorks( OtherIndexedEntity.NAME )
					.add( "4", b -> b
							.field( "numeric", entity4.getNumeric() )
							.field( "numericAsString", String.valueOf( entity4.getNumeric() ) )
					);
			backendMock.expectWorks( YetAnotherIndexedEntity.NAME )
					.add( "5", b -> b
							.field( "myLocalDateField", entity5.getLocalDate() )
							.field( "numeric", entity5.getNumeric() )
							.objectField( "myEmbeddedList", b2 -> b2
									.objectField( "myEmbedded", b3 -> b3
											.objectField( "customBridgeOnClass", b4 -> b4
													.field( "text", entity2.getEmbedded().getText() )
											)
									)
							)
							.objectField( "myEmbeddedList", b2 -> b2
									.objectField( "myEmbedded", b3 -> b3
											.objectField( "customBridgeOnClass", b4 -> b4
													.field( "text", entity3.getEmbedded().getText() )
											)
									)
							)
							.objectField( "myEmbeddedList", b2 -> {} )
							.objectField( "embeddedMap", b2 -> b2
									.objectField( "myEmbedded", b3 -> b3
											.field( "myLocalDateField", entity3.getEmbedded().getLocalDate() )
									)
							)
							.field( "embeddedMapKeys", "entity3", "entity2" )
							.objectField( "embeddedMap", b2 -> b2
									.objectField( "myEmbedded", b3 -> b3
											.field( "myLocalDateField", entity2.getEmbedded().getLocalDate() )
									)
							)
					);
		} );
	}

	@Test
	public void search() {
		backendMock.inLenientMode( () -> setupHolder.runInTransaction( session -> {
			IndexedEntity entity0 = new IndexedEntity();
			entity0.setId( 0 );
			session.persist( entity0 );
			YetAnotherIndexedEntity entity1 = new YetAnotherIndexedEntity();
			entity1.setId( 1 );
			session.persist( entity1 );
		} ) );

		setupHolder.runInTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			SearchQuery<ParentIndexedEntity> query = searchSession.search(
					Arrays.asList( IndexedEntity.class, YetAnotherIndexedEntity.class )
			)
					.selectEntity()
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME, YetAnotherIndexedEntity.NAME ),
					b -> b
							.offset( 3 )
							.limit( 2 ),
					StubSearchWorkBehavior.of(
							6L,
							reference( IndexedEntity.NAME, "0" ),
							reference( YetAnotherIndexedEntity.NAME, "1" )
					)
			);

			List<ParentIndexedEntity> result = query.fetchHits( 3, 2 );
			backendMock.verifyExpectationsMet();
			assertThat( result )
					.containsExactly(
							session.getReference( IndexedEntity.class, 0 ),
							session.getReference( YetAnotherIndexedEntity.class, 1 )
					);

			backendMock.expectCount( Arrays.asList( IndexedEntity.NAME, YetAnotherIndexedEntity.NAME ), 6L );

			long resultSize = query.fetchTotalHitCount();
			backendMock.verifyExpectationsMet();
			assertThat( resultSize ).isEqualTo( 6L );
		} );
	}

	private class MyMappingConfigurer implements HibernateOrmSearchMappingConfigurer {
		@Override
		public void configure(HibernateOrmMappingConfigurationContext context) {
			ProgrammaticMappingConfigurationContext mapping = context.programmaticMapping();
			TypeMappingStep indexedEntityMapping = mapping.type( IndexedEntity.class );
			indexedEntityMapping.indexed().index( IndexedEntity.NAME );
			indexedEntityMapping.binder(
					new CustomTypeBridge.Binder()
							.objectName( "customBridgeOnClass" )
			);
			indexedEntityMapping.property( "id" ).documentId();
			indexedEntityMapping.property( "text" ).genericField( "myTextField" );
			indexedEntityMapping.property( "embedded" )
					.indexedEmbedded( "myEmbedded" )
					.includeDepth( 1 )
					.includePaths( "customBridgeOnClass.text", "myEmbedded.customBridgeOnClass.text" );

			ProgrammaticMappingConfigurationContext secondMapping = context.programmaticMapping();

			TypeMappingStep parentIndexedEntityMapping = secondMapping.type( ParentIndexedEntity.class );
			parentIndexedEntityMapping.property( "localDate" )
					.genericField( "myLocalDateField" );
			parentIndexedEntityMapping.property( "embedded" )
					.binder(
							new CustomPropertyBridge.Binder()
									.objectName( "customBridgeOnProperty" )
					);

			TypeMappingStep otherIndexedEntityMapping = secondMapping.type( OtherIndexedEntity.class );
			otherIndexedEntityMapping.indexed().index( OtherIndexedEntity.NAME );
			otherIndexedEntityMapping.property( "id" )
					.documentId();
			otherIndexedEntityMapping.property( "numeric" )
					.genericField()
					.genericField( "numericAsString" ).valueBridge( IntegerAsStringValueBridge.class );

			TypeMappingStep yetAnotherIndexedEntityMapping = secondMapping.type( YetAnotherIndexedEntity.class );
			yetAnotherIndexedEntityMapping.indexed().index( YetAnotherIndexedEntity.NAME );
			yetAnotherIndexedEntityMapping.property( "id" ).documentId();
			yetAnotherIndexedEntityMapping.property( "numeric" ).genericField();
			yetAnotherIndexedEntityMapping.property( "embeddedList" )
					.indexedEmbedded( "myEmbeddedList" )
					.includePaths( "myEmbedded.customBridgeOnClass.text" );
			yetAnotherIndexedEntityMapping.property( "embeddedMap" )
					.genericField( "embeddedMapKeys" ).extractor( BuiltinContainerExtractors.MAP_KEY )
					.indexedEmbedded().includePaths( "myEmbedded.myLocalDateField" );
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

	@Entity(name = IndexedEntity.NAME)
	public static class IndexedEntity extends ParentIndexedEntity {

		public static final String NAME = "indexed";

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

	@Entity(name = OtherIndexedEntity.NAME)
	public static class OtherIndexedEntity {

		public static final String NAME = "other";

		@Id
		private Integer id;

		@Column(name = "numeric_column")
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

	@Entity(name = YetAnotherIndexedEntity.NAME)
	public static class YetAnotherIndexedEntity extends ParentIndexedEntity {

		public static final String NAME = "yetanother";

		@Id
		private Integer id;

		@Column(name = "numeric_column")
		private Integer numeric;

		@ManyToMany
		@JoinTable(name = "yetanother_indexed_list", joinColumns = @JoinColumn(name = "eListYAIndexed_id"))
		private List<IndexedEntity> embeddedList;

		@ManyToMany
		@JoinTable(name = "yetanother_indexed_map", joinColumns = @JoinColumn(name = "eMapYAIndexed_id"))
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

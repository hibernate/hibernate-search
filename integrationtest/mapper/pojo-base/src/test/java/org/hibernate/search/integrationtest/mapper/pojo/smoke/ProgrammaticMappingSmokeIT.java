/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.integrationtest.mapper.pojo.smoke.bridge.CustomPropertyBridge;
import org.hibernate.search.integrationtest.mapper.pojo.smoke.bridge.CustomTypeBridge;
import org.hibernate.search.integrationtest.mapper.pojo.smoke.bridge.IntegerAsStringValueBridge;
import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReference;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.scope.SearchScope;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ProgrammaticMappingSmokeIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@BeforeEach
	void setup() {
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
				.objectField( "embeddedIterable", b2 -> b2
						.multiValued( true )
						.objectField( "myEmbedded", b3 -> b3
								.field( "myTextField", String.class )
						)
				)
				.objectField( "myEmbeddedList", b2 -> b2
						.multiValued( true )
						.objectField( "myEmbedded", b3 -> b3
								.objectField( "customBridgeOnClass", b4 -> b4
										.field( "text", String.class )
								)
						)
				)
				.objectField( "embeddedArrayList", b2 -> b2
						.multiValued( true )
						.objectField( "myEmbedded", b3 -> b3
								.objectField( "customBridgeOnProperty", b4 -> b4
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

		mapping = setupHelper.start()
				.expectCustomBeans()
				.withConfiguration( builder -> {
					ProgrammaticMappingConfigurationContext mappingDefinition = builder.programmaticMapping();
					TypeMappingStep indexedEntityMapping = mappingDefinition.type( IndexedEntity.class );
					indexedEntityMapping.searchEntity().name( IndexedEntity.NAME );
					indexedEntityMapping.indexed();
					indexedEntityMapping.binder(
							new CustomTypeBridge.Binder()
									.objectName( "customBridgeOnClass" )
					);
					indexedEntityMapping.property( "id" ).documentId();
					indexedEntityMapping.property( "text" )
							.genericField( "myTextField" );
					indexedEntityMapping.property( "embedded" )
							.indexedEmbedded( "myEmbedded" )
							.includeDepth( 1 )
							.includePaths( "customBridgeOnClass.text", "myEmbedded.customBridgeOnClass.text" );

					ProgrammaticMappingConfigurationContext secondMappingDefinition = builder.programmaticMapping();

					TypeMappingStep parentIndexedEntityMapping = secondMappingDefinition.type( ParentIndexedEntity.class );
					parentIndexedEntityMapping.property( "localDate" )
							.genericField( "myLocalDateField" );
					parentIndexedEntityMapping.property( "embedded" )
							.associationInverseSide(
									PojoModelPath.ofValue( "embeddingAsSingle" )
							)
							.binder(
									new CustomPropertyBridge.Binder()
											.objectName( "customBridgeOnProperty" )
							);

					TypeMappingStep otherIndexedEntityMapping = secondMappingDefinition.type( OtherIndexedEntity.class );
					otherIndexedEntityMapping.searchEntity().name( OtherIndexedEntity.NAME );
					otherIndexedEntityMapping.indexed();
					otherIndexedEntityMapping.property( "id" )
							.documentId();
					otherIndexedEntityMapping.property( "numeric" )
							.genericField()
							.genericField( "numericAsString" ).valueBridge( IntegerAsStringValueBridge.class );

					TypeMappingStep yetAnotherIndexedEntityMapping =
							secondMappingDefinition.type( YetAnotherIndexedEntity.class );
					yetAnotherIndexedEntityMapping.searchEntity().name( YetAnotherIndexedEntity.NAME );
					yetAnotherIndexedEntityMapping.indexed();
					yetAnotherIndexedEntityMapping.property( "id" ).documentId();
					yetAnotherIndexedEntityMapping.property( "numeric" ).genericField();
					yetAnotherIndexedEntityMapping.property( "embeddedIterable" )
							.associationInverseSide(
									PojoModelPath.ofValue( "embeddingAsIterable" )
							)
							.indexedEmbedded().includePaths( "myEmbedded.myTextField" );
					yetAnotherIndexedEntityMapping.property( "embeddedList" )
							.associationInverseSide(
									PojoModelPath.ofValue( "embeddingAsList" )
							)
							.indexedEmbedded( "myEmbeddedList" )
							.includePaths( "myEmbedded.customBridgeOnClass.text" );
					yetAnotherIndexedEntityMapping.property( "embeddedArrayList" )
							.associationInverseSide(
									PojoModelPath.ofValue( "embeddingAsArrayList" )
							)
							.indexedEmbedded().includePaths( "myEmbedded.customBridgeOnProperty.text" );
					yetAnotherIndexedEntityMapping.property( "embeddedMap" )
							.associationInverseSide(
									PojoModelPath.ofValue( "embeddingAsMap" )
							)
							.genericField( "embeddedMapKeys" ).extractor( BuiltinContainerExtractors.MAP_KEY )
							.indexedEmbedded().includePaths( "myEmbedded.myLocalDateField" );
				} )
				.setup();

		backendMock.verifyExpectationsMet();
	}

	@Test
	void index() {
		try ( SearchSession session = mapping.createSession() ) {
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
			entity2.getEmbeddingAsSingle().add( entity1 );

			entity2.setEmbedded( entity3 );
			entity3.getEmbeddingAsSingle().add( entity2 );

			entity3.setEmbedded( entity2 );
			entity2.getEmbeddingAsSingle().add( entity3 );

			entity5.setEmbeddedIterable( new LinkedHashSet<>( Arrays.asList( entity1, entity2 ) ) );
			entity1.getEmbeddingAsIterable().add( entity5 );
			entity2.getEmbeddingAsIterable().add( entity5 );

			entity5.setEmbeddedList( Arrays.asList( entity2, entity3, entity6 ) );
			entity2.getEmbeddingAsList().add( entity5 );
			entity3.getEmbeddingAsList().add( entity5 );
			entity6.getEmbeddingAsList().add( entity5 );

			entity5.setEmbeddedArrayList( new ArrayList<>( Arrays.asList( entity3, entity1 ) ) );
			entity3.getEmbeddingAsArrayList().add( entity5 );
			entity1.getEmbeddingAsArrayList().add( entity5 );

			Map<String, List<IndexedEntity>> embeddedMap = new LinkedHashMap<>();
			embeddedMap.computeIfAbsent( "entity3", ignored -> new ArrayList<>() ).add( entity3 );
			embeddedMap.computeIfAbsent( "entity2", ignored -> new ArrayList<>() ).add( entity2 );
			embeddedMap.computeIfAbsent( "entity2", ignored -> new ArrayList<>() ).add( entity3 );
			entity5.setEmbeddedMap( embeddedMap );
			entity3.getEmbeddingAsMap().add( entity5 );
			entity2.getEmbeddingAsMap().add( entity5 );
			entity3.getEmbeddingAsMap().add( entity5 );

			session.indexingPlan().add( entity1 );
			session.indexingPlan().add( entity2 );
			session.indexingPlan().add( entity4 );
			session.indexingPlan().delete( entity1 );
			session.indexingPlan().add( entity3 );
			session.indexingPlan().add( entity5 );
			session.indexingPlan().add( entity6 );

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
							// entity1 was deleted, so it will be omitted
							.objectField( "embeddedIterable", b2 -> b2
									.objectField( "myEmbedded", b3 -> b3
											.field( "myTextField", entity2.getEmbedded().getText() )
									)
							)
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
							.objectField( "embeddedArrayList", b2 -> b2
									.objectField( "myEmbedded", b3 -> b3
											.objectField( "customBridgeOnProperty", b4 -> b4
													.field( "text", entity3.getEmbedded().getEmbedded().getText() )
											)
									)
							)
							// entity1 was deleted, so it will be omitted
							.field( "embeddedMapKeys", "entity3", "entity2" )
							.objectField( "embeddedMap", b2 -> b2
									.objectField( "myEmbedded", b3 -> b3
											.field( "myLocalDateField", entity3.getEmbedded().getLocalDate() )
									)
							)
							.objectField( "embeddedMap", b2 -> b2
									.objectField( "myEmbedded", b3 -> b3
											.field( "myLocalDateField", entity2.getEmbedded().getLocalDate() )
									)
							)
							.objectField( "embeddedMap", b2 -> b2
									.objectField( "myEmbedded", b3 -> b3
											.field( "myLocalDateField", entity3.getEmbedded().getLocalDate() )
									)
							)
					);
		}
	}

	@Test
	void search() {
		try ( SearchSession session = mapping.createSession() ) {
			SearchQuery<EntityReference> query = session.search(
					Arrays.asList( IndexedEntity.class, YetAnotherIndexedEntity.class )
			)
					.selectEntityReference()
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchReferences(
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


			SearchResult<EntityReference> result = query.fetch( 3, 2 );
			assertThat( result.hits() )
					.containsExactly(
							PojoEntityReference.withDefaultName( IndexedEntity.class, 0 ),
							PojoEntityReference.withDefaultName( YetAnotherIndexedEntity.class, 1 )
					);
			assertThat( result.total().hitCount() ).isEqualTo( 6L );

			backendMock.verifyExpectationsMet();
		}
	}

	@Test
	void search_singleElementProjection() {
		try ( SearchSession session = mapping.createSession() ) {
			SearchQuery<String> query = session.search(
					Arrays.asList( IndexedEntity.class, YetAnotherIndexedEntity.class )
			)
					.select( f -> f.field( "myTextField", String.class ) )
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchProjection(
					Arrays.asList( IndexedEntity.NAME, YetAnotherIndexedEntity.NAME ),
					b -> b
							.offset( 3 )
							.limit( 2 ),
					StubSearchWorkBehavior.of(
							2L,
							"text1",
							null
					)
			);

			SearchResult<String> result = query.fetch( 3, 2 );
			assertThat( result.hits() )
					.containsExactly(
							"text1",
							null
					);
			assertThat( result.total().hitCount() ).isEqualTo( 2L );

			backendMock.verifyExpectationsMet();
		}
	}

	@Test
	void search_multipleElementsProjection() {
		SearchScope<ParentIndexedEntity> scope = mapping.scope(
				Arrays.asList( IndexedEntity.class, YetAnotherIndexedEntity.class )
		);

		try ( SearchSession session = mapping.createSession() ) {
			SearchQuery<List<?>> query = session.search( scope )
					.select(
							scope.projection().field( "myTextField", String.class ).toProjection(),
							scope.projection().entityReference().toProjection(),
							scope.projection().id( Integer.class ).toProjection(),
							scope.projection().field( "myLocalDateField", LocalDate.class ).toProjection(),
							scope.projection().documentReference().toProjection(),
							scope.projection().field( "customBridgeOnClass.text", String.class ).toProjection()
					)
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchProjection(
					Arrays.asList( IndexedEntity.NAME, YetAnotherIndexedEntity.NAME ),
					StubSearchWorkBehavior.of(
							2L,
							Arrays.asList(
									"text1",
									reference( IndexedEntity.NAME, "0" ),
									"0",
									LocalDate.of( 2017, 11, 1 ),
									reference( IndexedEntity.NAME, "0" ),
									"text2"
							),
							Arrays.asList(
									null,
									reference( YetAnotherIndexedEntity.NAME, "1" ),
									"1",
									LocalDate.of( 2017, 11, 2 ),
									reference( YetAnotherIndexedEntity.NAME, "1" ),
									null
							)
					)
			);

			SearchResult<List<?>> result = query.fetchAll();
			assertThat( result.hits() )
					.containsExactly(
							Arrays.asList(
									"text1",
									PojoEntityReference.withDefaultName( IndexedEntity.class, 0 ),
									0,
									LocalDate.of( 2017, 11, 1 ),
									reference( IndexedEntity.NAME, "0" ),
									"text2"
							),
							Arrays.asList(
									null,
									PojoEntityReference.withDefaultName( YetAnotherIndexedEntity.class, 1 ),
									1,
									LocalDate.of( 2017, 11, 2 ),
									reference( YetAnotherIndexedEntity.NAME, "1" ),
									null
							)
					);
			assertThat( result.total().hitCount() ).isEqualTo( 2L );

			backendMock.verifyExpectationsMet();
		}
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

		public static final String NAME = "IndexedEntity";

		private Integer id;

		private String text;

		private List<ParentIndexedEntity> embeddingAsSingle = new ArrayList<>();

		private List<YetAnotherIndexedEntity> embeddingAsIterable = new ArrayList<>();

		private List<YetAnotherIndexedEntity> embeddingAsList = new ArrayList<>();

		private List<YetAnotherIndexedEntity> embeddingAsArrayList = new ArrayList<>();

		private List<YetAnotherIndexedEntity> embeddingAsMap = new ArrayList<>();

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

		public List<ParentIndexedEntity> getEmbeddingAsSingle() {
			return embeddingAsSingle;
		}

		public List<YetAnotherIndexedEntity> getEmbeddingAsIterable() {
			return embeddingAsIterable;
		}

		public List<YetAnotherIndexedEntity> getEmbeddingAsList() {
			return embeddingAsList;
		}

		public List<YetAnotherIndexedEntity> getEmbeddingAsArrayList() {
			return embeddingAsArrayList;
		}

		public List<YetAnotherIndexedEntity> getEmbeddingAsMap() {
			return embeddingAsMap;
		}
	}

	public static final class OtherIndexedEntity {

		public static final String NAME = "OtherIndexedEntity";

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

		public static final String NAME = "YetAnotherIndexedEntity";

		private Integer id;

		private Integer numeric;

		private Iterable<IndexedEntity> embeddedIterable;

		private List<IndexedEntity> embeddedList;

		private ArrayList<IndexedEntity> embeddedArrayList;

		private Map<String, List<IndexedEntity>> embeddedMap;

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

		public Iterable<IndexedEntity> getEmbeddedIterable() {
			return embeddedIterable;
		}

		public void setEmbeddedIterable(Iterable<IndexedEntity> embeddedIterable) {
			this.embeddedIterable = embeddedIterable;
		}

		public List<IndexedEntity> getEmbeddedList() {
			return embeddedList;
		}

		public void setEmbeddedList(List<IndexedEntity> embeddedList) {
			this.embeddedList = embeddedList;
		}

		public ArrayList<IndexedEntity> getEmbeddedArrayList() {
			return embeddedArrayList;
		}

		public void setEmbeddedArrayList(ArrayList<IndexedEntity> embeddedArrayList) {
			this.embeddedArrayList = embeddedArrayList;
		}

		public Map<String, List<IndexedEntity>> getEmbeddedMap() {
			return embeddedMap;
		}

		public void setEmbeddedMap(Map<String, List<IndexedEntity>> embeddedMap) {
			this.embeddedMap = embeddedMap;
		}
	}

}

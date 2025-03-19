/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.metamodel.standalone.lucene;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.VectorField;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBinder;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.scope.SearchScope;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PredicateTypesIT {

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withSingleBackend(
					MethodHandles.lookup(), new LuceneBackendConfiguration() );

	private SearchMapping mapping;

	private final Map<Long, IndexedEntity> simulatedIndexedEntityDatastore = new HashMap<>();

	@BeforeEach
	void setup() {
		mapping = setupHelper.start()
				.withAnnotatedTypes( IndexedEntity.class )
				.withProperty( StandalonePojoMapperSettings.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY,
						IndexingPlanSynchronizationStrategyNames.SYNC )
				.withConfiguration( b -> b.programmaticMapping()
						.type( IndexedEntity.class )
						.searchEntity()
						.loadingBinder( (EntityLoadingBinder) c -> {
							c.selectionLoadingStrategy( IndexedEntity.class,
									SelectionLoadingStrategy.fromMap( simulatedIndexedEntityDatastore ) );
						} ) )
				.withConfiguration( b -> b.defaultReindexOnUpdate( ReindexOnUpdate.SHALLOW ) )
				.setup();
	}

	@Test
	void smoke() {
		try ( SearchSession session = mapping.createSessionWithOptions()
				.indexingPlanSynchronizationStrategy( IndexingPlanSynchronizationStrategy.sync() )
				.build() ) {
			IndexedEntity entity1 = new IndexedEntity( 1L );
			IndexedEntity entity2 = new IndexedEntity( 2L );
			session.indexingPlan().add( entity1 );
			session.indexingPlan().add( entity2 );
			simulatedIndexedEntityDatastore.put( entity1.id, entity1 );
			simulatedIndexedEntityDatastore.put( entity2.id, entity2 );
		}

		try ( SearchSession session = mapping.createSession() ) {
			SearchScope<PredicateTypesIT_IndexedEntity__, IndexedEntity> scope =
					PredicateTypesIT_IndexedEntity__.INDEX.scope( session );
			assertThat( session.search( scope )
					.where( f -> f.bool()
							.should( f.match().field( PredicateTypesIT_IndexedEntity__.INDEX.text ).matching( "text" ) )
							.should( f.range().field( PredicateTypesIT_IndexedEntity__.INDEX.text ).atLeast( "text" ) )
							.should( f.phrase().field( PredicateTypesIT_IndexedEntity__.INDEX.text ).matching( "text" ) )
							.should( f.exists().field( PredicateTypesIT_IndexedEntity__.INDEX.text ) )
							.should( f.wildcard().field( PredicateTypesIT_IndexedEntity__.INDEX.text ).matching( "?ext" ) )
							.should( f.regexp().field( PredicateTypesIT_IndexedEntity__.INDEX.text ).matching( "?ext" ) )
							.should( f.terms().field( PredicateTypesIT_IndexedEntity__.INDEX.text ).matchingAny( "text" ) )
							.should( f.simpleQueryString().field( PredicateTypesIT_IndexedEntity__.INDEX.text )
									.matching( "text" ) )
							.should( f.queryString().field( PredicateTypesIT_IndexedEntity__.INDEX.text ).matching( "text" ) )
							.should( f.prefix().field( PredicateTypesIT_IndexedEntity__.INDEX.text ).matching( "te" ) )

							.should( f.spatial().within().field( PredicateTypesIT_IndexedEntity__.INDEX.geoPoint )
									.circle( GeoPoint.of( 10.0, 20.0 ), 20.0 ) )
							.should( f.exists().field( PredicateTypesIT_IndexedEntity__.INDEX.geoPoint ) )

							.should( f.knn( 10 ).field( PredicateTypesIT_IndexedEntity__.INDEX.floatVector )
									.matching( new float[] { 1.0f, 2.0f, 3.0f } ) )
							.should( f.exists().field( PredicateTypesIT_IndexedEntity__.INDEX.floatVector ) )

							.should( f.match().field( PredicateTypesIT_IndexedEntity__.INDEX.myEnum ).matching( MyEnum.B ) )
							.should( f.range().field( PredicateTypesIT_IndexedEntity__.INDEX.myEnum ).atLeast( MyEnum.B ) )
							.should( f.phrase().field( PredicateTypesIT_IndexedEntity__.INDEX.myEnum ).matching( "B" ) )
							.should( f.exists().field( PredicateTypesIT_IndexedEntity__.INDEX.myEnum ) )
							.should( f.wildcard().field( PredicateTypesIT_IndexedEntity__.INDEX.myEnum ).matching( "?" ) )
							.should( f.regexp().field( PredicateTypesIT_IndexedEntity__.INDEX.myEnum ).matching( ".+" ) )
							.should( f.terms().field( PredicateTypesIT_IndexedEntity__.INDEX.myEnum ).matchingAny( MyEnum.B ) )
							.should( f.simpleQueryString().field( PredicateTypesIT_IndexedEntity__.INDEX.myEnum )
									.matching( "B" ) )
							.should( f.queryString().field( PredicateTypesIT_IndexedEntity__.INDEX.myEnum ).matching( "B" ) )
							.should( f.prefix().field( PredicateTypesIT_IndexedEntity__.INDEX.myEnum ).matching( "B" ) )
					)
					.fetchHits( 20 ) )
					.hasSize( 2 );
		}
	}

	@SearchEntity(name = "IndexedEntity")
	@Indexed
	public static class IndexedEntity {
		@DocumentId
		public Long id;
		@FullTextField
		public String text;
		@GenericField
		public GeoPoint geoPoint;
		@VectorField(dimension = 3)
		public float[] floatVector;
		@KeywordField
		public MyEnum myEnum;

		public IndexedEntity() {
		}

		public IndexedEntity(Long id) {
			this.id = id;
			this.text = "text";
			this.geoPoint = GeoPoint.of( 10.0, 20.0 );
			this.floatVector = new float[] { 1.0f, 2.0f, 3.0f };
			this.myEnum = MyEnum.B;
		}
	}

	public enum MyEnum {
		A, B, C, D;
	}

}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.metamodel.standalone.lucene;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBinder;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.scope.TypedSearchScope;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class EntityAsTreeSmokeIT {

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withSingleBackend(
					MethodHandles.lookup(), new LuceneBackendConfiguration() );

	private SearchMapping mapping;

	private final Map<String, IndexedEntity> simulatedIndexedEntityDatastore = new HashMap<>();

	@BeforeEach
	void setup() {
		mapping = setupHelper.start()
				.withAnnotatedTypes( ContainedNonEntity.class, IndexedEntity.class, ContainedEntity.class )
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
	void indexAndSearch() {
		IndexedEntity indexed1 = new IndexedEntity( "1", "some interesting text" );
		ContainedEntity containedEntity1_1 = new ContainedEntity( "1_1", "some contained entity text" );
		containedEntity1_1.containing = indexed1;
		indexed1.containedEntities.add( containedEntity1_1 );
		ContainedNonEntity containedNonEntity1_1 = new ContainedNonEntity( "some contained nonentity text" );
		indexed1.containedNonEntities.add( containedNonEntity1_1 );

		IndexedEntity indexed2 = new IndexedEntity( "2", "some other text" );
		ContainedEntity containedEntity2_1 = new ContainedEntity( "2_1", "some other text" );
		containedEntity2_1.containing = indexed2;
		indexed2.containedEntities.add( containedEntity2_1 );
		ContainedNonEntity containedNonEntity2_1 = new ContainedNonEntity( "some other text" );
		indexed2.containedNonEntities.add( containedNonEntity2_1 );

		try ( SearchSession session = mapping.createSession() ) {
			assertThat( EntityAsTreeSmokeIT_IndexedEntity__.INDEX.search( session )
					.where( f -> f.match().field( EntityAsTreeSmokeIT_IndexedEntity__.INDEX.containedEntities.text )
							.matching( "entity text" ) )
					.fetchHits( 20 ) )
					.isEmpty();
		}
		try ( SearchSession session = mapping.createSessionWithOptions()
				.indexingPlanSynchronizationStrategy( IndexingPlanSynchronizationStrategy.sync() )
				.build() ) {
			session.indexingPlan().add( indexed1 );
			session.indexingPlan().add( indexed2 );
			simulatedIndexedEntityDatastore.put( indexed1.id, indexed1 );
			simulatedIndexedEntityDatastore.put( indexed2.id, indexed2 );
		}
		try ( SearchSession session = mapping.createSession() ) {
			TypedSearchScope<EntityAsTreeSmokeIT_IndexedEntity__, IndexedEntity> scope =
					EntityAsTreeSmokeIT_IndexedEntity__.INDEX.scope( session );
			assertThat( session.search( scope )
					.where( f -> f.match().field( EntityAsTreeSmokeIT_IndexedEntity__.INDEX.containedEntities.text )
							.matching( "entity" ) )
					.fetchHits( 20 ) )
					.containsExactlyInAnyOrder( indexed1 );
		}
		try ( SearchSession session = mapping.createSessionWithOptions()
				.indexingPlanSynchronizationStrategy( IndexingPlanSynchronizationStrategy.sync() )
				.build() ) {
			session.indexingPlan().delete( indexed1 );
		}
		try ( SearchSession session = mapping.createSession() ) {
			TypedSearchScope<EntityAsTreeSmokeIT_IndexedEntity__, IndexedEntity> scope =
					EntityAsTreeSmokeIT_IndexedEntity__.INDEX.scope( session );
			assertThat( session.search( scope )
					.where( f -> f.match().field( EntityAsTreeSmokeIT_IndexedEntity__.INDEX.containedEntities.text )
							.matching( "entity text" ) )
					.fetchHits( 20 ) )
					.containsExactlyInAnyOrder( indexed2 );
		}
	}

	@SearchEntity
	@Indexed
	public static class IndexedEntity {
		@DocumentId
		public String id;
		@FullTextField(projectable = Projectable.YES)
		public String text;
		@IndexedEmbedded
		public List<ContainedEntity> containedEntities = new ArrayList<>();
		@IndexedEmbedded
		public List<ContainedNonEntity> containedNonEntities = new ArrayList<>();

		public IndexedEntity(String id, String text) {
			this.id = id;
			this.text = text;
		}
	}

	@SearchEntity
	public static class ContainedEntity {
		// Not setting @DocumentId here because it shouldn't be necessary
		public String id;
		@FullTextField
		public String text;
		public IndexedEntity containing;

		public ContainedEntity(String id, String text) {
			this.id = id;
			this.text = text;
		}
	}

	public static class ContainedNonEntity {
		@FullTextField(projectable = Projectable.YES)
		public String text;

		public ContainedNonEntity(String text) {
			this.text = text;
		}
	}
}

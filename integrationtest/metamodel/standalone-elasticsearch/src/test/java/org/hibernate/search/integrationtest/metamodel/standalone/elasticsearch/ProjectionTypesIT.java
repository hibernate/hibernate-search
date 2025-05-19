/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.metamodel.standalone.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBinder;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.scope.TypedSearchScope;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ProjectionTypesIT {

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withSingleBackend(
					MethodHandles.lookup(), new ElasticsearchBackendConfiguration() );

	private SearchMapping mapping;

	private final Map<Long, IndexedEntity> simulatedIndexedEntityDatastore = new HashMap<>();

	@BeforeEach
	void setup() {
		mapping = setupHelper.start()
				.withAnnotatedTypes( IndexedEntity.class, ContainedEntity.class )
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
			TypedSearchScope<ProjectionTypesIT_IndexedEntity__, IndexedEntity> scope =
					ProjectionTypesIT_IndexedEntity__.INDEX.scope( session );
			assertThat( session.search( scope )
					.select( f -> f.composite()
							.from(
									f.field( ProjectionTypesIT_IndexedEntity__.INDEX.text ),
									f.field( ProjectionTypesIT_IndexedEntity__.INDEX.number ),
									f.field( ProjectionTypesIT_IndexedEntity__.INDEX.myDate ),
									f.field( ProjectionTypesIT_IndexedEntity__.INDEX.contained.text ).list()
							).asArray() )
					.where( f -> f.matchAll() )
					.fetchHits( 20 ) )
					.hasSize( 2 );
		}
	}

	@SearchEntity(name = "IndexedEntity")
	@Indexed
	public static class IndexedEntity {
		@DocumentId
		public Long id;
		@KeywordField(projectable = Projectable.YES)
		public String text;
		@GenericField(projectable = Projectable.YES)
		public int number;
		@GenericField(projectable = Projectable.YES)
		public LocalDate myDate;
		@IndexedEmbedded
		public Set<ContainedEntity> contained;

		public IndexedEntity() {
		}

		public IndexedEntity(Long id) {
			this.id = id;
			this.text = "text";
			this.myDate = LocalDate.of( 2000, 1, 1 );
			this.contained = new HashSet<>();
			this.contained.add( new ContainedEntity( id + 100, this ) );
		}
	}

	public static class ContainedEntity {
		// Not setting @DocumentId here because it shouldn't be necessary
		public Long id;
		@FullTextField(projectable = Projectable.YES)
		public String text;
		public IndexedEntity containing;

		public ContainedEntity() {
		}

		public ContainedEntity(Long id, IndexedEntity indexedEntity) {
			this.id = id;
			this.text = "contained text";
			this.containing = indexedEntity;
		}
	}
}

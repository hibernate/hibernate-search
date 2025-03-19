/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.metamodel.standalone.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBinder;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.scope.SearchScope;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SortTypesIT {

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withSingleBackend(
					MethodHandles.lookup(), new ElasticsearchBackendConfiguration() );

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
			SearchScope<SortTypesIT_IndexedEntity__, IndexedEntity> scope =
					SortTypesIT_IndexedEntity__.INDEX.scope( session );
			assertThat( session.search( scope )
					.where( f -> f.matchAll() )
					.sort( f -> f.field( SortTypesIT_IndexedEntity__.INDEX.number ).asc().missing().use( 5 ) )
					.fetchHits( 20 ) )
					.hasSize( 2 );

			assertThat( session.search( scope )
					.where( f -> f.matchAll() )
					.sort( f -> f.field( SortTypesIT_IndexedEntity__.INDEX.myDate ).asc().missing()
							.use( LocalDate.of( 2000, 1, 1 ) ) )
					.fetchHits( 20 ) )
					.hasSize( 2 );
		}
	}

	@SearchEntity(name = "IndexedEntity")
	@Indexed
	public static class IndexedEntity {
		@DocumentId
		public Long id;
		@KeywordField(sortable = Sortable.YES)
		public String text;
		@GenericField(sortable = Sortable.YES)
		public int number;
		@GenericField(sortable = Sortable.YES)
		public LocalDate myDate;

		public IndexedEntity() {
		}

		public IndexedEntity(Long id) {
			this.id = id;
			this.text = "text";
		}
	}
}

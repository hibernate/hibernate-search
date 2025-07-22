/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.metamodel.standalone.lucene;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
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
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class AggregationTypesIT {

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
			assertThat( AggregationTypesIT_IndexedEntity__.INDEX.search( session )
					.where( f -> f.matchAll() )
					.aggregation( AggregationKey.of( "range" ),
							f -> f.terms().field( AggregationTypesIT_IndexedEntity__.INDEX.text ) )

					.aggregation( AggregationKey.of( "sum" ),
							f -> f.sum().field( AggregationTypesIT_IndexedEntity__.INDEX.number ) )
					.aggregation( AggregationKey.of( "count" ),
							f -> f.count().field( AggregationTypesIT_IndexedEntity__.INDEX.number ) )
					.aggregation( AggregationKey.of( "countDistinct" ),
							f -> f.count().field( AggregationTypesIT_IndexedEntity__.INDEX.number ).distinct() )
					.aggregation( AggregationKey.of( "min" ),
							f -> f.min().field( AggregationTypesIT_IndexedEntity__.INDEX.number ) )
					.aggregation( AggregationKey.of( "max" ),
							f -> f.max().field( AggregationTypesIT_IndexedEntity__.INDEX.number ) )
					.aggregation( AggregationKey.of( "avg" ),
							f -> f.avg().field( AggregationTypesIT_IndexedEntity__.INDEX.number ) )
					.aggregation( AggregationKey.of( "terms" ),
							f -> f.terms().field( AggregationTypesIT_IndexedEntity__.INDEX.number ) )
					.aggregation( AggregationKey.of( "range2" ),
							f -> f.range().field( AggregationTypesIT_IndexedEntity__.INDEX.number ).range( 1, 2 ) )

					.aggregation( AggregationKey.of( "range3" ),
							f -> f.range().field( AggregationTypesIT_IndexedEntity__.INDEX.myDate ).range( LocalDate.MIN,
									LocalDate.MAX ) )

					.fetchHits( 20 ) )
					.hasSize( 2 );
		}
	}

	@SearchEntity(name = "IndexedEntity")
	@Indexed
	public static class IndexedEntity {
		@DocumentId
		public Long id;
		@KeywordField(aggregable = Aggregable.YES)
		public String text;
		@GenericField(aggregable = Aggregable.YES)
		public int number;
		@GenericField(aggregable = Aggregable.YES)
		public LocalDate myDate;

		public IndexedEntity() {
		}

		public IndexedEntity(Long id) {
			this.id = id;
			this.text = "text";
		}
	}
}

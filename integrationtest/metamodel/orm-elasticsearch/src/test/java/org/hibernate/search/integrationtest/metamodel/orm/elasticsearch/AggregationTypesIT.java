/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.metamodel.orm.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.scope.TypedSearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class AggregationTypesIT {

	@RegisterExtension
	public OrmSetupHelper setupHelper = OrmSetupHelper.withSingleBackend( new ElasticsearchBackendConfiguration() );

	private SessionFactory sessionFactory;

	@BeforeEach
	void setup() {
		sessionFactory = setupHelper.start()
				.withAnnotatedTypes( IndexedEntity.class )
				.withProperty( HibernateOrmMapperSettings.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY,
						IndexingPlanSynchronizationStrategyNames.SYNC )
				.setup();
	}

	@Test
	void smoke() {
		with( sessionFactory ).runInTransaction( session -> {
			session.persist( new IndexedEntity( 1L ) );
			session.persist( new IndexedEntity( 2L ) );
		} );

		try ( var s = sessionFactory.openSession() ) {
			SearchSession session = Search.session( s );
			TypedSearchScope<AggregationTypesIT_IndexedEntity__, IndexedEntity> scope =
					AggregationTypesIT_IndexedEntity__.INDEX.scope( session );
			assertThat( session.search( scope )
					.where( f -> f.matchAll() )
					.aggregation( AggregationKey.of( "range" ),
							f -> f.terms().field( AggregationTypesIT_IndexedEntity__.INDEX.myText ) )

					.aggregation( AggregationKey.of( "sum" ),
							f -> f.sum().field( AggregationTypesIT_IndexedEntity__.INDEX.myNumber ) )
					.aggregation( AggregationKey.of( "count" ),
							f -> f.count().field( AggregationTypesIT_IndexedEntity__.INDEX.myNumber ) )
					.aggregation( AggregationKey.of( "countDistinct" ),
							f -> f.countDistinct().field( AggregationTypesIT_IndexedEntity__.INDEX.myNumber ) )
					.aggregation( AggregationKey.of( "min" ),
							f -> f.min().field( AggregationTypesIT_IndexedEntity__.INDEX.myNumber ) )
					.aggregation( AggregationKey.of( "max" ),
							f -> f.max().field( AggregationTypesIT_IndexedEntity__.INDEX.myNumber ) )
					.aggregation( AggregationKey.of( "avg" ),
							f -> f.avg().field( AggregationTypesIT_IndexedEntity__.INDEX.myNumber ) )
					.aggregation( AggregationKey.of( "terms" ),
							f -> f.terms().field( AggregationTypesIT_IndexedEntity__.INDEX.myNumber ) )
					.aggregation( AggregationKey.of( "range2" ),
							f -> f.range().field( AggregationTypesIT_IndexedEntity__.INDEX.myNumber )
									.range( 1, 20 )
									.range( 20, 50 ) )

					// NOTE: OpenSearch has issues with having this range aggregation with other range ones >_<
					//  hence we'll test it in the other query:
					// .aggregation( AggregationKey.of( "range3" ),
					// 		f -> f.range().field( AggregationTypesIT_IndexedEntity__.INDEX.myDate )
					// 				.range( LocalDate.of( 1999, 1, 1 ),
					// 						LocalDate.of( 2000, 1, 1 ) )
					// 				.range( LocalDate.of( 2000, 1, 1 ),
					// 						LocalDate.of( 2010, 1, 1 ) ))

					.fetchHits( 20 ) )
					.hasSize( 2 );

			assertThat( session.search( scope )
					.where( f -> f.matchAll() )
					.aggregation( AggregationKey.of( "range3" ),
							f -> f.range().field( AggregationTypesIT_IndexedEntity__.INDEX.myDate )
									.range( LocalDate.of( 1999, 1, 1 ),
											LocalDate.of( 2000, 1, 1 ) )
									.range( LocalDate.of( 2000, 1, 1 ),
											LocalDate.of( 2010, 1, 1 ) )
					)
					.fetchHits( 20 ) )
					.hasSize( 2 );
		}
	}

	@Entity(name = "IndexedEntity")
	@Indexed
	public static class IndexedEntity {
		@Id
		public Long id;
		@KeywordField(aggregable = Aggregable.YES)
		public String myText;
		@GenericField(aggregable = Aggregable.YES)
		public int myNumber;
		@GenericField(aggregable = Aggregable.YES)
		public LocalDate myDate;

		public IndexedEntity() {
		}

		public IndexedEntity(Long id) {
			this.id = id;
			this.myText = "text";
		}
	}
}

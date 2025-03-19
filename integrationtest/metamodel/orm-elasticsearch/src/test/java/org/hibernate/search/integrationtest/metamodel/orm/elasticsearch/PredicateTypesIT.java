/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.metamodel.orm.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.VectorField;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PredicateTypesIT {

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

	@Entity(name = "IndexedEntity")
	@Indexed
	public static class IndexedEntity {
		@Id
		public Long id;
		@FullTextField
		public String text;
		@Transient
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)
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

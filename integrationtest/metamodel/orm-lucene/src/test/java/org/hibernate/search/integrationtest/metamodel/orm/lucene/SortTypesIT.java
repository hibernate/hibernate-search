/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.metamodel.orm.lucene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.scope.TypedSearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SortTypesIT {

	@RegisterExtension
	public OrmSetupHelper setupHelper = OrmSetupHelper.withSingleBackend( new LuceneBackendConfiguration() );

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
			TypedSearchScope<SortTypesIT_IndexedEntity__, IndexedEntity> scope =
					SortTypesIT_IndexedEntity__.INDEX.scope( session );
			assertThat( session.search( scope )
					.where( f -> f.matchAll() )
					.sort( f -> f.field( SortTypesIT_IndexedEntity__.INDEX.myNumber ).asc().missing().use( 5 ) )
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

	@Entity(name = "IndexedEntity")
	@Indexed
	public static class IndexedEntity {
		@Id
		public Long id;
		@KeywordField(sortable = Sortable.YES)
		public String myText;
		@GenericField(sortable = Sortable.YES)
		public int myNumber;
		@GenericField(sortable = Sortable.YES)
		public LocalDate myDate;

		public IndexedEntity() {
		}

		public IndexedEntity(Long id) {
			this.id = id;
			this.myText = "text";
		}
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.metamodel.orm.lucene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ProjectionTypesIT {

	@RegisterExtension
	public OrmSetupHelper setupHelper = OrmSetupHelper.withSingleBackend( new LuceneBackendConfiguration() );

	private SessionFactory sessionFactory;

	@BeforeEach
	void setup() {
		sessionFactory = setupHelper.start()
				.withAnnotatedTypes( IndexedEntity.class, ContainedEntity.class )
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
			SearchScope<ProjectionTypesIT_IndexedEntity__, IndexedEntity> scope =
					ProjectionTypesIT_IndexedEntity__.INDEX.scope( session );
			assertThat( session.search( scope )
					.select( f -> f.composite()
							.from(
									f.field( ProjectionTypesIT_IndexedEntity__.INDEX.myText ),
									f.field( ProjectionTypesIT_IndexedEntity__.INDEX.myNumber ),
									f.field( ProjectionTypesIT_IndexedEntity__.INDEX.myDate ),
									f.field( ProjectionTypesIT_IndexedEntity__.INDEX.contained.text ).list()
							).asArray() )
					.where( f -> f.matchAll() )
					.fetchHits( 20 ) )
					.hasSize( 2 );
		}
	}

	@Entity(name = "IndexedEntity")
	@Indexed
	public static class IndexedEntity {
		@Id
		public Long id;
		@KeywordField(projectable = Projectable.YES)
		public String myText;
		@GenericField(projectable = Projectable.YES)
		public int myNumber;
		@GenericField(projectable = Projectable.YES)
		public LocalDate myDate;
		@IndexedEmbedded
		@OneToMany(mappedBy = "containing")
		public Set<ContainedEntity> contained;

		public IndexedEntity() {
		}

		public IndexedEntity(Long id) {
			this.id = id;
			this.myText = "text";
			this.myDate = LocalDate.of( 2000, 1, 1 );
			this.contained = new HashSet<>();
			this.contained.add( new ContainedEntity( id + 100, this ) );
		}
	}

	@Entity
	public static class ContainedEntity {
		// Not setting @DocumentId here because it shouldn't be necessary
		@Id
		public Long id;
		@FullTextField(projectable = Projectable.YES)
		public String text;
		@ManyToOne
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

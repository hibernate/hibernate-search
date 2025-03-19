/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.metamodel.standalone.lucene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class EntityAsTreeSmokeIT {

	@RegisterExtension
	public OrmSetupHelper setupHelper = OrmSetupHelper.withSingleBackend( new LuceneBackendConfiguration() );

	private SessionFactory sessionFactory;

	@BeforeEach
	void setup() {
		sessionFactory = setupHelper.start()
				.withAnnotatedTypes( ContainedNonEntity.class, IndexedEntity.class, ContainedEntity.class )
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

		try ( var s = sessionFactory.openSession() ) {
			SearchSession session = Search.session( s );
			SearchScope<EntityAsTreeSmokeIT_IndexedEntity__, IndexedEntity> scope = EntityAsTreeSmokeIT_IndexedEntity__.INDEX.scope( session );
			assertThat( session.search( scope )
					.where( f -> f.match().field( EntityAsTreeSmokeIT_IndexedEntity__.INDEX.containedEntities.text )
							.matching( "entity text" ) )
					.fetchHits( 20 ) )
					.isEmpty();
		}

		with( sessionFactory ).runInTransaction( session -> {
			session.persist( indexed1 );
			session.persist( containedEntity1_1 );
			session.persist( indexed2 );
		} );

		try ( var s = sessionFactory.openSession() ) {
			SearchSession session = Search.session( s );
			SearchScope<EntityAsTreeSmokeIT_IndexedEntity__, IndexedEntity> scope = EntityAsTreeSmokeIT_IndexedEntity__.INDEX.scope( session );
			assertThat( session.search( scope )
					.select( f -> f.id() )
					.where( f -> f.match().field( EntityAsTreeSmokeIT_IndexedEntity__.INDEX.containedEntities.text )
							.matching( "entity" ) )
					.fetchHits( 20 ) )
					.containsExactlyInAnyOrder( indexed1.id );
		}
	}

	@Entity
	@Indexed
	public static class IndexedEntity {
		@Id
		public String id;
		@FullTextField(projectable = Projectable.YES)
		public String text;
		@OneToMany(mappedBy = "containing")
		@IndexedEmbedded
		public List<ContainedEntity> containedEntities = new ArrayList<>();
		@ElementCollection
		@IndexedEmbedded
		public List<ContainedNonEntity> containedNonEntities = new ArrayList<>();

		public IndexedEntity() {
		}

		public IndexedEntity(String id, String text) {
			this.id = id;
			this.text = text;
		}
	}

	@Entity
	public static class ContainedEntity {
		// Not setting @DocumentId here because it shouldn't be necessary
		@Id
		public String id;
		@FullTextField
		public String text;
		@ManyToOne
		public IndexedEntity containing;

		public ContainedEntity() {
		}

		public ContainedEntity(String id, String text) {
			this.id = id;
			this.text = text;
		}
	}

	@Embeddable
	public static class ContainedNonEntity {
		@FullTextField(projectable = Projectable.YES)
		public String text;

		public ContainedNonEntity() {
		}

		public ContainedNonEntity(String text) {
			this.text = text;
		}
	}
}

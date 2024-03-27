/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Check that the JPA @Id is correctly used as the default @DocumentId.
 */
class JpaIdAsDocumentIdIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Test
	void test() {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.objectField( "contained", b2 -> b2
						.multiValued( true )
						.field( "id", Integer.class, b3 -> b3
								.searchable( Searchable.YES ).projectable( Projectable.YES ) ) ) );

		SessionFactory sessionFactory = ormSetupHelper.start()
				.setup( IndexedEntity.class, ContainedEntity.class );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 0 );
			ContainedEntity contained1 = new ContainedEntity();
			contained1.setId( 1 );
			entity1.getContained().add( contained1 );
			contained1.containing = entity1;
			ContainedEntity contained2 = new ContainedEntity();
			contained2.setId( 2 );
			entity1.getContained().add( contained2 );
			contained2.containing = entity1;

			session.persist( entity1 );
			session.persist( contained1 );
			session.persist( contained2 );

			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "0", b -> b
							.objectField( "contained", b2 -> b2
									.field( "id", 1 ) )
							.objectField( "contained", b2 -> b2
									.field( "id", 2 ) ) );
		} );
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static final class IndexedEntity {
		static final String NAME = "indexed";

		@Id
		private Integer id;

		@OneToMany(mappedBy = "containing")
		@OrderColumn
		@IndexedEmbedded(includeEmbeddedObjectId = true)
		private List<ContainedEntity> contained = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<ContainedEntity> getContained() {
			return contained;
		}

		public void setContained(List<ContainedEntity> contained) {
			this.contained = contained;
		}
	}

	@Entity(name = ContainedEntity.NAME)
	public static final class ContainedEntity {
		static final String NAME = "contained";

		@Id
		private Integer id;

		@ManyToOne
		private IndexedEntity containing;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public IndexedEntity getContaining() {
			return containing;
		}

		public void setContaining(IndexedEntity containing) {
			this.containing = containing;
		}
	}

}

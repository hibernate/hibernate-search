/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutomaticIndexingConcurrentModificationInSameTypeIT {

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );
	private SessionFactory sessionFactory;

	@BeforeAll
	void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "firstName", String.class )
				.field( "name", String.class )
				.objectField( "child", b2 -> b2
						.field( "firstName", String.class )
						.field( "name", String.class )
				)
		);

		sessionFactory = ormSetupHelper.start().withAnnotatedTypes( IndexedEntity.class ).setup();
	}

	@BeforeEach
	void initData() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setName( "edouard" );
			entity1.setFirstName( "zobocare" );

			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setName( "yann" );
			entity2.setFirstName( "bonduel" );

			IndexedEntity entity3 = new IndexedEntity();
			entity3.setId( 3 );
			entity3.setName( "antoine" );
			entity3.setFirstName( "owl" );

			entity2.setChild( entity1 );
			entity1.setParent( entity2 );

			session.persist( entity1 );
			session.persist( entity2 );
			session.persist( entity3 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( String.valueOf( 1 ), b -> b
							.field( "name", "edouard" )
							.field( "firstName", "zobocare" ) )
					.add( String.valueOf( 2 ), b -> b
							.field( "name", "yann" )
							.field( "firstName", "bonduel" )
							.objectField( "child", b2 -> b2
									.field( "name", "edouard" )
									.field( "firstName", "zobocare" ) ) )
					.add( String.valueOf( 3 ), b -> b
							.field( "name", "antoine" )
							.field( "firstName", "owl" ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3857")
	public void updateTriggeringReindexingOfPreviouslyUnknownEntity() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.getReference( IndexedEntity.class, 1 );
			entity1.setName( "updated" );
			// Add another entity to the indexing plan so that we're not done iterating over all entities
			// when entity2 is added to the indexing plan due to the change in the child.
			IndexedEntity entity3 = session.getReference( IndexedEntity.class, 3 );
			entity3.setName( "updated" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( String.valueOf( 1 ), b -> b
							.field( "name", "updated" )
							.field( "firstName", "zobocare" ) )
					.addOrUpdate( String.valueOf( 2 ), b -> b
							.field( "name", "yann" )
							.field( "firstName", "bonduel" )
							.objectField( "child", b2 -> b2
									.field( "name", "updated" )
									.field( "firstName", "zobocare" ) ) )
					.addOrUpdate( String.valueOf( 3 ), b -> b
							.field( "name", "updated" )
							.field( "firstName", "owl" ) );
		} );

		backendMock.verifyExpectationsMet();
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {
		static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@GenericField
		@Basic
		private String firstName;

		@GenericField
		@Basic
		private String name;

		@OneToOne
		private IndexedEntity parent;

		@IndexedEmbedded(includeDepth = 1)
		@OneToOne(mappedBy = "parent")
		private IndexedEntity child;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public IndexedEntity getParent() {
			return parent;
		}

		public void setParent(IndexedEntity parent) {
			this.parent = parent;
		}

		public IndexedEntity getChild() {
			return child;
		}

		public void setChild(IndexedEntity child) {
			this.child = child;
		}
	}
}

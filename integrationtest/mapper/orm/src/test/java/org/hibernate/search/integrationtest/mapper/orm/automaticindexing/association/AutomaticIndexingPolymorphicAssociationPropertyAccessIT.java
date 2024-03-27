/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-1241")
@PortedFromSearch5(original = "org.hibernate.search.test.embedded.polymorphism.PolymorphicAssociationPropertyAccessTest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutomaticIndexingPolymorphicAssociationPropertyAccessIT {

	private static final String INIT_NAME = "initname";
	private static final String EDIT_NAME = "editname";

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	protected SessionFactory sessionFactory;

	@BeforeAll
	void setup() {
		backendMock.expectAnySchema( Level1.NAME );
		backendMock.expectAnySchema( Level2.NAME );
		backendMock.expectAnySchema( DerivedLevel2.NAME );
		backendMock.expectAnySchema( Level3.NAME );
		sessionFactory = ormSetupHelper.start()
				.dataClearing( config -> config.clearOrder( Level3.class, DerivedLevel2.class, Level2.class, Level1.class ) )
				.setup( Level1.class, Level2.class, DerivedLevel2.class, Level3.class );
	}

	@Test
	void testPolymorphicAssociation() {
		with( sessionFactory ).runInTransaction( session -> {
			Level1 level1 = new Level1();
			level1.setId( 1 );
			DerivedLevel2 level2 = new DerivedLevel2();
			level2.setId( 2 );
			Level3 level3 = new Level3();
			level3.setId( 3 );

			level1.setLevel2Child( level2 );
			level2.setLevel1Parent( level1 );
			level2.setLevel3Child( level3 );
			level3.setLevel2Parent( level2 );

			level3.setName( INIT_NAME );

			session.persist( level1 );
			session.persist( level2 );
			session.persist( level3 );

			backendMock.expectWorks( Level1.NAME )
					.add( "1", b -> b.objectField( "level2Child", b2 -> b2
							.objectField( "level3Child", b3 -> b3
									.field( "name", INIT_NAME ) ) ) );

			backendMock.expectWorks( DerivedLevel2.NAME )
					.add( "2", b -> b.objectField( "level3Child", b3 -> b3
							.field( "name", INIT_NAME ) ) );

			backendMock.expectWorks( Level3.NAME )
					.add( "3", b -> b.field( "name", INIT_NAME ) );
		} );
		backendMock.verifyExpectationsMet();

		// Here the test can fail if the polymorphic association is not properly handled.
		// In this case, an exception will be thrown because of a failed field access on a proxy object.
		with( sessionFactory ).runInTransaction( session -> {
			// Must use getReference to get a proxy and reproduce the problem
			Level3 level3 = session.getReference( Level3.class, 3 );
			level3.setName( EDIT_NAME );

			backendMock.expectWorks( Level1.NAME )
					.addOrUpdate( "1", b -> b.objectField( "level2Child", b2 -> b2
							.objectField( "level3Child", b3 -> b3
									.field( "name", EDIT_NAME ) ) ) );

			backendMock.expectWorks( DerivedLevel2.NAME )
					.addOrUpdate( "2", b -> b.objectField( "level3Child", b3 -> b3
							.field( "name", EDIT_NAME ) ) );

			backendMock.expectWorks( Level3.NAME )
					.addOrUpdate( "3", b -> b.field( "name", EDIT_NAME ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = Level1.NAME)
	@Indexed
	static class Level1 {
		public static final String NAME = "Level1";

		@Id
		private Integer id;

		@OneToOne(mappedBy = "level1Parent")
		@IndexedEmbedded
		private DerivedLevel2 level2Child;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public DerivedLevel2 getLevel2Child() {
			return level2Child;
		}

		public void setLevel2Child(DerivedLevel2 level2Child) {
			this.level2Child = level2Child;
		}

	}

	@Entity(name = Level2.NAME)
	@Indexed
	static class Level2 {
		public static final String NAME = "Level2";

		@Id
		private Integer id;

		@OneToOne(mappedBy = "level2Parent")
		@IndexedEmbedded
		private Level3 level3Child;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Level3 getLevel3Child() {
			return level3Child;
		}

		public void setLevel3Child(Level3 level3Child) {
			this.level3Child = level3Child;
		}

	}

	@Entity(name = DerivedLevel2.NAME)
	@Indexed
	static class DerivedLevel2 extends Level2 {
		public static final String NAME = "DerivedLevel2";

		@OneToOne
		private Level1 level1Parent;

		public Level1 getLevel1Parent() {
			return level1Parent;
		}

		public void setLevel1Parent(Level1 level1Parent) {
			this.level1Parent = level1Parent;
		}

	}

	@Entity(name = Level3.NAME)
	@Indexed
	static class Level3 {
		public static final String NAME = "Level3";

		@Id
		private Integer id;

		@FullTextField
		private String name;

		@OneToOne(fetch = FetchType.LAZY)
		private Level2 level2Parent;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Level2 getLevel2Parent() {
			return level2Parent;
		}

		public void setLevel2Parent(Level2 level2Parent) {
			this.level2Parent = level2Parent;
		}

	}
}

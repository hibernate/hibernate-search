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

@TestForIssue(jiraKey = "HSEARCH-3156")
@PortedFromSearch5(original = "org.hibernate.search.test.embedded.polymorphism.PolymorphicAssociationContainedInTargetTest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutomaticIndexingPolymorphicAssociationIT {

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
		sessionFactory = ormSetupHelper.start()
				.dataClearing( config -> config.clearOrder( Level3.class, DerivedLevel2.class, Level2.class, Level1.class ) )
				.setup( Level1.class, Level2.class, DerivedLevel2.class, Level3.class );
	}

	// Nominal case: the level3 refers to a level2 of a contained type (DerivedLevel2).
	@Test
	void testPolymorphicAssociationConfiguredType() {
		with( sessionFactory ).runInTransaction( session -> {
			Level1 level1 = new Level1();
			level1.setId( 1 );

			DerivedLevel2 derivedLevel2 = new DerivedLevel2();
			derivedLevel2.setId( 2 );
			Level3 derivedLevel2Level3 = new Level3();
			derivedLevel2Level3.setId( 3 );
			derivedLevel2Level3.setName( INIT_NAME );

			level1.setDerivedLevel2Child( derivedLevel2 );
			derivedLevel2.setLevel1Parent( level1 );
			derivedLevel2.setLevel3Child( derivedLevel2Level3 );
			derivedLevel2Level3.setLevel2Parent( derivedLevel2 );

			session.persist( level1 );
			session.persist( derivedLevel2 );
			session.persist( derivedLevel2Level3 );

			backendMock.expectWorks( Level1.NAME )
					.add( "1", b -> b.objectField( "derivedLevel2Child", b2 -> b2
							.objectField( "level3Child", b3 -> b3
									.field( "name", INIT_NAME ) ) ) );
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			Level3 derivedLevel2Level3 = session.get( Level3.class, 3 );
			derivedLevel2Level3.setName( EDIT_NAME );

			backendMock.expectWorks( Level1.NAME )
					.addOrUpdate( "1", b -> b.objectField( "derivedLevel2Child", b2 -> b2
							.objectField( "level3Child", b3 -> b3
									.field( "name", EDIT_NAME ) ) ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	// Failing case: the level3 refers to a level2 of a non-contained type (Level2, the base type).
	// This should not affect the index, but indexing used to fail with an exception before HSEARCH-3156 was solved.
	@Test
	void testPolymorphicAssociationNonContainedType() {
		with( sessionFactory ).runInTransaction( session -> {
			Level1 level1 = new Level1();
			level1.setId( 1 );

			Level2 baseLevel2 = new Level2();
			baseLevel2.setId( 4 );
			Level3 baseLevel2Level3 = new Level3();
			baseLevel2Level3.setId( 5 );
			baseLevel2Level3.setName( INIT_NAME );

			baseLevel2.setLevel3Child( baseLevel2Level3 );
			baseLevel2Level3.setLevel2Parent( baseLevel2 );

			session.persist( level1 );
			session.persist( baseLevel2 );
			session.persist( baseLevel2Level3 );

			backendMock.expectWorks( Level1.NAME )
					.add( "1", b -> {} );
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			Level3 baseLevel2Level3 = session.get( Level3.class, 5 );
			baseLevel2Level3.setName( EDIT_NAME );
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
		private DerivedLevel2 derivedLevel2Child;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public DerivedLevel2 getDerivedLevel2Child() {
			return derivedLevel2Child;
		}

		public void setDerivedLevel2Child(DerivedLevel2 derivedLevel2Child) {
			this.derivedLevel2Child = derivedLevel2Child;
		}
	}

	@Entity(name = "Level2")
	static class Level2 {

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

	@Entity(name = "DerivedLevel2")
	static class DerivedLevel2 extends Level2 {

		@OneToOne
		private Level1 level1Parent;

		public Level1 getLevel1Parent() {
			return level1Parent;
		}

		public void setLevel1Parent(Level1 level1Parent) {
			this.level1Parent = level1Parent;
		}

	}

	@Entity(name = "Level3")
	static class Level3 {

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

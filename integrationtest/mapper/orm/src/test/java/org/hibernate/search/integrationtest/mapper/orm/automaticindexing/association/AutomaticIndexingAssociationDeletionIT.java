/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test that Hibernate Search does not throw a {@link org.hibernate.LazyInitializationException}
 * when executing automatic indexing after one side of a non-cascading associations was deleted,
 * especially if that side of the association has an ElementCollection
 * that will trigger a PostCollectionRemove event (which counts as an update for the deleted entity...).
 * <p>
 * This type of deletion may trigger reindexing resolution (because of the PostCollectionRemove event,
 * or simply because Hibernate Search resolves reindexing on deletion),
 * and the deleted entity ends up being processed to find associated entities to reindex,
 * which may lead to attempts to initialize association collections
 * that can no longer be initialized due to one of the entities involved having been deleted.
 * Thus, we take extra care to catch {@link org.hibernate.LazyInitializationException} and ignore it.
 */
@TestForIssue(jiraKey = "HSEARCH-3999")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AutomaticIndexingAssociationDeletionIT {

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	protected SessionFactory sessionFactory;

	@BeforeAll
	void setup() {
		backendMock.expectAnySchema( AssociationOwner.NAME );
		backendMock.expectAnySchema( AssociationNonOwner.NAME );
		OrmSetupHelper.SetupContext setupContext = ormSetupHelper.start().withAnnotatedTypes(
				AssociationOwner.class, AssociationNonOwner.class )
				.dataClearing( config -> config.clearOrder( AssociationOwner.class,
						AssociationNonOwner.class ) );

		sessionFactory = additionalSetup( setupContext ).setup();
	}

	protected OrmSetupHelper.SetupContext additionalSetup(OrmSetupHelper.SetupContext setupContext) {
		return setupContext;
	}

	protected OrmSetupHelper.SetupContext configure(OrmSetupHelper.SetupContext ctx) {
		return ctx;
	}

	@Test
	void optionalOneToOne_deleteOwner() {
		initOptionalOneToOne();

		with( sessionFactory ).runInTransaction( session -> {
			AssociationOwner owner1 = session.getReference( AssociationOwner.class, 1 );

			session.remove( owner1 );

			backendMock.expectWorks( AssociationOwner.NAME )
					.delete( "1" );

			backendMock.expectWorks( AssociationNonOwner.NAME )
					.addOrUpdate( "2", b -> b.field( "basic", "text 2" )
							.field( "elementCollection", 1002, 2002 ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void optionalOneToOne_deleteNonOwner() {
		initOptionalOneToOne();

		with( sessionFactory ).runInTransaction( session -> {
			AssociationOwner owner1 = session.getReference( AssociationOwner.class, 1 );
			AssociationNonOwner nonOwner2 = session.getReference( AssociationNonOwner.class, 2 );

			// Necessary because the foreign key will no longer reference an existing row.
			owner1.setOptionalOneToOne( null );

			session.remove( nonOwner2 );

			// This update is caused by the call to owner1.setOptionalOneToOne;
			// it has nothing to do with the deletion.
			backendMock.expectWorks( AssociationOwner.NAME )
					.addOrUpdate( "1", b -> b.field( "basic", "text 1" )
							.field( "elementCollection", 1001, 2001 ) );

			backendMock.expectWorks( AssociationNonOwner.NAME )
					.delete( "2" );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void optionalOneToOne_deleteBoth() {
		initOptionalOneToOne();

		with( sessionFactory ).runInTransaction( session -> {
			AssociationOwner owner1 = session.getReference( AssociationOwner.class, 1 );
			AssociationNonOwner nonOwner2 = session.getReference( AssociationNonOwner.class, 2 );

			session.remove( owner1 );
			session.remove( nonOwner2 );

			backendMock.expectWorks( AssociationOwner.NAME )
					.delete( "1" );

			backendMock.expectWorks( AssociationNonOwner.NAME )
					.delete( "2" );
		} );
		backendMock.verifyExpectationsMet();
	}

	private void initOptionalOneToOne() {
		with( sessionFactory ).runInTransaction( session -> {
			AssociationOwner owner1 = new AssociationOwner( 1 );
			AssociationNonOwner nonOwner2 = new AssociationNonOwner( 2 );

			owner1.setOptionalOneToOne( nonOwner2 );
			nonOwner2.setOptionalOneToOne( owner1 );

			session.persist( owner1 );
			session.persist( nonOwner2 );

			backendMock.expectWorks( AssociationOwner.NAME )
					.add( "1", b -> b.field( "basic", "text 1" )
							.field( "elementCollection", 1001, 2001 )
							.objectField( "optionalOneToOne", b2 -> b2
									.field( "basic", "text 2" )
									.field( "elementCollection", 1002, 2002 ) ) );
			backendMock.expectWorks( AssociationNonOwner.NAME )
					.add( "2", b -> b.field( "basic", "text 2" )
							.field( "elementCollection", 1002, 2002 )
							.objectField( "optionalOneToOne", b2 -> b2
									.field( "basic", "text 1" )
									.field( "elementCollection", 1001, 2001 ) ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void manyToOne_deleteOwner() {
		initManyToOne();

		with( sessionFactory ).runInTransaction( session -> {
			AssociationOwner owner1 = session.getReference( AssociationOwner.class, 1 );

			session.remove( owner1 );

			backendMock.expectWorks( AssociationOwner.NAME )
					.delete( "1" );

			backendMock.expectWorks( AssociationNonOwner.NAME )
					.addOrUpdate( "2", b -> b.field( "basic", "text 2" )
							.field( "elementCollection", 1002, 2002 )
							.objectField( "oneToMany", b2 -> b2
									.field( "basic", "text 3" )
									.field( "elementCollection", 1003, 2003 ) ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void manyToOne_deleteNonOwner() {
		initManyToOne();

		with( sessionFactory ).runInTransaction( session -> {
			AssociationOwner owner1 = session.getReference( AssociationOwner.class, 1 );
			AssociationOwner owner3 = session.getReference( AssociationOwner.class, 3 );
			AssociationNonOwner nonOwner2 = session.getReference( AssociationNonOwner.class, 2 );

			// Necessary because the foreign key will no longer reference an existing row.
			owner1.setManyToOne( null );
			owner3.setManyToOne( null );

			session.remove( nonOwner2 );

			backendMock.expectWorks( AssociationOwner.NAME )
					// This update is caused by the call to owner1.setManyToOne;
					// it has nothing to do with the deletion.
					.addOrUpdate( "1", b -> b.field( "basic", "text 1" )
							.field( "elementCollection", 1001, 2001 ) )
					// This update is caused by the call to owner3.setManyToOne;
					// it has nothing to do with the deletion.
					.addOrUpdate( "3", b -> b.field( "basic", "text 3" )
							.field( "elementCollection", 1003, 2003 ) );

			backendMock.expectWorks( AssociationNonOwner.NAME )
					.delete( "2" );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void manyToOne_deleteBoth() {
		initManyToOne();

		with( sessionFactory ).runInTransaction( session -> {
			AssociationOwner owner3 = session.getReference( AssociationOwner.class, 3 );
			AssociationOwner owner1 = session.getReference( AssociationOwner.class, 1 );
			AssociationNonOwner nonOwner2 = session.getReference( AssociationNonOwner.class, 2 );

			// Necessary because the foreign key will no longer reference an existing row.
			owner3.setManyToOne( null );

			session.remove( owner1 );
			session.remove( nonOwner2 );

			backendMock.expectWorks( AssociationOwner.NAME )
					// This update is caused by the call to owner3.setManyToOne;
					// it has nothing to do with the deletion.
					.addOrUpdate( "3", b -> b.field( "basic", "text 3" )
							.field( "elementCollection", 1003, 2003 ) )
					.delete( "1" );

			backendMock.expectWorks( AssociationNonOwner.NAME )
					.delete( "2" );
		} );
		backendMock.verifyExpectationsMet();
	}

	private void initManyToOne() {
		with( sessionFactory ).runInTransaction( session -> {
			AssociationOwner owner1 = new AssociationOwner( 1 );
			AssociationNonOwner nonOwner2 = new AssociationNonOwner( 2 );
			AssociationOwner owner3 = new AssociationOwner( 3 );

			owner1.setManyToOne( nonOwner2 );
			nonOwner2.getOneToMany().add( owner1 );
			owner3.setManyToOne( nonOwner2 );
			nonOwner2.getOneToMany().add( owner3 );

			session.persist( owner1 );
			session.persist( nonOwner2 );
			session.persist( owner3 );

			backendMock.expectWorks( AssociationOwner.NAME )
					.add( "1", b -> b.field( "basic", "text 1" )
							.field( "elementCollection", 1001, 2001 )
							.objectField( "manyToOne", b2 -> b2
									.field( "basic", "text 2" )
									.field( "elementCollection", 1002, 2002 ) ) )
					.add( "3", b -> b.field( "basic", "text 3" )
							.field( "elementCollection", 1003, 2003 )
							.objectField( "manyToOne", b2 -> b2
									.field( "basic", "text 2" )
									.field( "elementCollection", 1002, 2002 ) ) );
			backendMock.expectWorks( AssociationNonOwner.NAME )
					.add( "2", b -> b.field( "basic", "text 2" )
							.field( "elementCollection", 1002, 2002 )
							.objectField( "oneToMany", b2 -> b2
									.field( "basic", "text 1" )
									.field( "elementCollection", 1001, 2001 ) )
							.objectField( "oneToMany", b2 -> b2
									.field( "basic", "text 3" )
									.field( "elementCollection", 1003, 2003 ) ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void manyToMany_deleteOwner() {
		initManyToMany();

		with( sessionFactory ).runInTransaction( session -> {
			AssociationOwner owner1 = session.getReference( AssociationOwner.class, 1 );

			session.remove( owner1 );

			backendMock.expectWorks( AssociationOwner.NAME )
					.delete( "1" );

			// We don't expect any update of the containing entity (id 2),
			// since its association to 1 was not updated
			// (the code above is technically incorrect).
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void manyToMany_deleteNonOwner() {
		initManyToMany();

		with( sessionFactory ).runInTransaction( session -> {
			AssociationOwner owner1 = session.getReference( AssociationOwner.class, 1 );
			AssociationOwner owner3 = session.getReference( AssociationOwner.class, 3 );
			AssociationNonOwner nonOwner2 = session.getReference( AssociationNonOwner.class, 2 );

			// Necessary because the foreign key will no longer reference an existing row.
			owner1.getManyToMany().remove( nonOwner2 );
			owner3.getManyToMany().remove( nonOwner2 );

			session.remove( nonOwner2 );

			backendMock.expectWorks( AssociationOwner.NAME )
					// This update is caused by the call to owner1.getManyToMany().remove();
					// it has nothing to do with the deletion.
					.addOrUpdate( "1", b -> b.field( "basic", "text 1" )
							.field( "elementCollection", 1001, 2001 )
							.objectField( "manyToMany", b2 -> b2
									.field( "basic", "text 4" )
									.field( "elementCollection", 1004, 2004 ) ) )
					// This update is caused by the call to owner3.getManyToMany().remove();
					// it has nothing to do with the deletion.
					.addOrUpdate( "3", b -> b.field( "basic", "text 3" )
							.field( "elementCollection", 1003, 2003 )
							.objectField( "manyToMany", b2 -> b2
									.field( "basic", "text 4" )
									.field( "elementCollection", 1004, 2004 ) ) );

			backendMock.expectWorks( AssociationNonOwner.NAME )
					.delete( "2" );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void manyToMany_deleteBoth() {
		initManyToMany();

		with( sessionFactory ).runInTransaction( session -> {
			AssociationOwner owner3 = session.getReference( AssociationOwner.class, 3 );
			AssociationOwner owner1 = session.getReference( AssociationOwner.class, 1 );
			AssociationNonOwner nonOwner2 = session.getReference( AssociationNonOwner.class, 2 );

			// Necessary because the foreign key will no longer reference an existing row.
			owner3.getManyToMany().remove( nonOwner2 );

			session.remove( owner1 );
			session.remove( nonOwner2 );

			backendMock.expectWorks( AssociationOwner.NAME )
					.delete( "1" )
					// This update is caused by the call to owner3.getManyToMany().remove;
					// it has nothing to do with the deletion.
					.addOrUpdate( "3", b -> b.field( "basic", "text 3" )
							.field( "elementCollection", 1003, 2003 )
							.objectField( "manyToMany", b2 -> b2
									.field( "basic", "text 4" )
									.field( "elementCollection", 1004, 2004 ) ) );

			backendMock.expectWorks( AssociationNonOwner.NAME )
					.delete( "2" );
			// We don't expect any update of the containing entity (id 4),
			// since its association to 1 was not updated
			// (the code above is technically incorrect).
		} );
		backendMock.verifyExpectationsMet();
	}

	private void initManyToMany() {
		with( sessionFactory ).runInTransaction( session -> {
			AssociationOwner owner1 = new AssociationOwner( 1 );
			AssociationNonOwner nonOwner2 = new AssociationNonOwner( 2 );
			AssociationOwner owner3 = new AssociationOwner( 3 );
			AssociationNonOwner nonOwner4 = new AssociationNonOwner( 4 );

			owner1.getManyToMany().add( nonOwner2 );
			owner1.getManyToMany().add( nonOwner4 );
			nonOwner2.getManyToMany().add( owner1 );
			nonOwner2.getManyToMany().add( owner3 );
			owner3.getManyToMany().add( nonOwner2 );
			owner3.getManyToMany().add( nonOwner4 );
			nonOwner4.getManyToMany().add( owner1 );
			nonOwner4.getManyToMany().add( owner3 );

			session.persist( nonOwner2 );
			session.persist( nonOwner4 );
			session.persist( owner1 );
			session.persist( owner3 );

			backendMock.expectWorks( AssociationOwner.NAME )
					.add( "1", b -> b.field( "basic", "text 1" )
							.field( "elementCollection", 1001, 2001 )
							.objectField( "manyToMany", b2 -> b2
									.field( "basic", "text 2" )
									.field( "elementCollection", 1002, 2002 ) )
							.objectField( "manyToMany", b2 -> b2
									.field( "basic", "text 4" )
									.field( "elementCollection", 1004, 2004 ) ) )
					.add( "3", b -> b.field( "basic", "text 3" )
							.field( "elementCollection", 1003, 2003 )
							.objectField( "manyToMany", b2 -> b2
									.field( "basic", "text 2" )
									.field( "elementCollection", 1002, 2002 ) )
							.objectField( "manyToMany", b2 -> b2
									.field( "basic", "text 4" )
									.field( "elementCollection", 1004, 2004 ) ) );
			backendMock.expectWorks( AssociationNonOwner.NAME )
					.add( "2", b -> b.field( "basic", "text 2" )
							.field( "elementCollection", 1002, 2002 )
							.objectField( "manyToMany", b2 -> b2
									.field( "basic", "text 1" )
									.field( "elementCollection", 1001, 2001 ) )
							.objectField( "manyToMany", b2 -> b2
									.field( "basic", "text 3" )
									.field( "elementCollection", 1003, 2003 ) ) )
					.add( "4", b -> b.field( "basic", "text 4" )
							.field( "elementCollection", 1004, 2004 )
							.objectField( "manyToMany", b2 -> b2
									.field( "basic", "text 1" )
									.field( "elementCollection", 1001, 2001 ) )
							.objectField( "manyToMany", b2 -> b2
									.field( "basic", "text 3" )
									.field( "elementCollection", 1003, 2003 ) ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = AssociationOwner.NAME)
	@Indexed
	public static class AssociationOwner {
		static final String NAME = "owner";

		@Id
		private Integer id;

		@GenericField
		@Basic
		private String basic;

		// This triggers a PostCollectionRemove event upon deletion, which may impact HSearch's behavior.
		@GenericField
		@ElementCollection
		@OrderBy
		private List<Integer> elementCollection;

		@IndexedEmbedded(includePaths = { "basic", "elementCollection" })
		@OneToOne(fetch = FetchType.LAZY, // Will be ignored except in the test extending this one and using bytecode enhancement.
				optional = true)
		private AssociationNonOwner optionalOneToOne;

		@IndexedEmbedded(includePaths = { "basic", "elementCollection" })
		@ManyToOne
		private AssociationNonOwner manyToOne;

		@IndexedEmbedded(includePaths = { "basic", "elementCollection" })
		@ManyToMany
		@JoinTable(joinColumns = @JoinColumn(name = "nonowner_id"),
				inverseJoinColumns = @JoinColumn(name = "owner_id"))
		@OrderColumn(name = "idx")
		private List<AssociationNonOwner> manyToMany = new ArrayList<>();

		AssociationOwner() {
		}

		AssociationOwner(int id) {
			this.id = id;
			this.basic = "text " + id;
			this.elementCollection = Arrays.asList( 1000 + id, 2000 + id );
		}

		public Integer getId() {
			return id;
		}

		public String getBasic() {
			return basic;
		}

		public void setBasic(String basic) {
			this.basic = basic;
		}

		public AssociationNonOwner getOptionalOneToOne() {
			return optionalOneToOne;
		}

		public void setOptionalOneToOne(AssociationNonOwner optionalOneToOne) {
			this.optionalOneToOne = optionalOneToOne;
		}

		public AssociationNonOwner getManyToOne() {
			return manyToOne;
		}

		public void setManyToOne(AssociationNonOwner manyToOne) {
			this.manyToOne = manyToOne;
		}

		public List<AssociationNonOwner> getManyToMany() {
			return manyToMany;
		}

		public void setManyToMany(List<AssociationNonOwner> manyToMany) {
			this.manyToMany = manyToMany;
		}
	}

	@Entity(name = AssociationNonOwner.NAME)
	@Indexed
	public static class AssociationNonOwner {
		static final String NAME = "nonowner";

		@Id
		private Integer id;

		@GenericField
		@Basic
		private String basic;

		// This triggers a PostCollectionRemove event upon deletion, which may impact HSearch's behavior.
		@GenericField
		@ElementCollection
		@OrderBy
		private List<Integer> elementCollection;

		@IndexedEmbedded(includePaths = { "basic", "elementCollection" })
		@OneToOne(fetch = FetchType.LAZY, // Will be ignored except in the test extending this one and using bytecode enhancement.
				mappedBy = "optionalOneToOne", optional = true)
		private AssociationOwner optionalOneToOne;

		@IndexedEmbedded(includePaths = { "basic", "elementCollection" })
		@OneToMany(mappedBy = "manyToOne")
		@OrderColumn(name = "idx")
		private List<AssociationOwner> oneToMany = new ArrayList<>();

		@IndexedEmbedded(includePaths = { "basic", "elementCollection" })
		@ManyToMany(mappedBy = "manyToMany")
		@OrderBy("id")
		private List<AssociationOwner> manyToMany = new ArrayList<>();

		AssociationNonOwner() {
		}

		AssociationNonOwner(int id) {
			this.id = id;
			this.basic = "text " + id;
			this.elementCollection = Arrays.asList( 1000 + id, 2000 + id );
		}

		public Integer getId() {
			return id;
		}

		public String getBasic() {
			return basic;
		}

		public void setBasic(String basic) {
			this.basic = basic;
		}

		public AssociationOwner getOptionalOneToOne() {
			return optionalOneToOne;
		}

		public void setOptionalOneToOne(AssociationOwner optionalOneToOne) {
			this.optionalOneToOne = optionalOneToOne;
		}

		public List<AssociationOwner> getOneToMany() {
			return oneToMany;
		}

		public void setOneToMany(List<AssociationOwner> oneToMany) {
			this.oneToMany = oneToMany;
		}

		public List<AssociationOwner> getManyToMany() {
			return manyToMany;
		}

		public void setManyToMany(List<AssociationOwner> manyToMany) {
			this.manyToMany = manyToMany;
		}
	}
}

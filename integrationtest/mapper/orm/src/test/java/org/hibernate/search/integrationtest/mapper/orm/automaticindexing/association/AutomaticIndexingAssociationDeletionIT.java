/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinTransaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.OrderColumn;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test that Hibernate Search does not throw a {@link org.hibernate.LazyInitializationException}
 * when executing automatic indexing after one side of an non-cascading associations was deleted,
 * especially if that side of the association has an ElementCollection
 * that will trigger a PostCollectionRemove event (which counts as an update for the deleted entity...).
 * <p>
 * This type of deletion used to trigger an update (because of the PostCollectionRemove event),
 * and the deleted entity ended up being processed to find associated entities to reindex,
 * which led to attempts to initialize association collections that no longer could be initialized
 * due to one of the entities involved having been deleted.
 */
@TestForIssue(jiraKey = "HSEARCH-3999")
public class AutomaticIndexingAssociationDeletionIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void before() {
		backendMock.expectAnySchema( AssociationOwner.NAME );
		backendMock.expectAnySchema( AssociationNonOwner.NAME );
		sessionFactory = ormSetupHelper
				.start()
				.withProperty( AvailableSettings.ALLOW_UPDATE_OUTSIDE_TRANSACTION, true )
				.setup( AssociationOwner.class, AssociationNonOwner.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void optionalOneToOne_deleteOwner() {
		initOptionalOneToOne();

		withinTransaction( sessionFactory, session -> {
			AssociationOwner owner1 = session.getReference( AssociationOwner.class, 1 );

			session.delete( owner1 );

			backendMock.expectWorks( AssociationOwner.NAME )
					.delete( "1" )
					.processedThenExecuted();

			// We don't expect any update of the containing entity (id 2),
			// since its association to 1 was not updated
			// (the code above is technically incorrect).
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void optionalOneToOne_deleteNonOwner() {
		initOptionalOneToOne();

		withinTransaction( sessionFactory, session -> {
			AssociationOwner owner1 = session.getReference( AssociationOwner.class, 1 );
			AssociationNonOwner nonOwner2 = session.getReference( AssociationNonOwner.class, 2 );

			// Necessary because the foreign key will no longer reference an existing row.
			owner1.setOptionalOneToOne( null );

			session.delete( nonOwner2 );

			// This update is caused by the call to owner1.setOptionalOneToOne;
			// it has nothing to do with the deletion.
			backendMock.expectWorks( AssociationOwner.NAME )
					.update( "1", b -> b.field( "basic", "text 1" )
							.field( "elementCollection", 1001, 2001 ) )
					.processedThenExecuted();

			backendMock.expectWorks( AssociationNonOwner.NAME )
					.delete( "2" )
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void optionalOneToOne_deleteBoth() {
		initOptionalOneToOne();

		withinTransaction( sessionFactory, session -> {
			AssociationOwner owner1 = session.getReference( AssociationOwner.class, 1 );
			AssociationNonOwner nonOwner2 = session.getReference( AssociationNonOwner.class, 2 );

			session.delete( owner1 );
			session.delete( nonOwner2 );

			backendMock.expectWorks( AssociationOwner.NAME )
					.delete( "1" )
					.processedThenExecuted();

			backendMock.expectWorks( AssociationNonOwner.NAME )
					.delete( "2" )
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	private void initOptionalOneToOne() {
		withinTransaction( sessionFactory, session -> {
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
									.field( "elementCollection", 1002, 2002 ) ) )
					.processedThenExecuted();
			backendMock.expectWorks( AssociationNonOwner.NAME )
					.add( "2", b -> b.field( "basic", "text 2" )
							.field( "elementCollection", 1002, 2002 )
							.objectField( "optionalOneToOne", b2 -> b2
									.field( "basic", "text 1" )
									.field( "elementCollection", 1001, 2001 ) ) )
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void manyToOne_deleteOwner() {
		initManyToOne();

		withinTransaction( sessionFactory, session -> {
			AssociationOwner owner1 = session.getReference( AssociationOwner.class, 1 );

			session.delete( owner1 );

			backendMock.expectWorks( AssociationOwner.NAME )
					.delete( "1" )
					.processedThenExecuted();

			// We don't expect any update of the containing entity (id 2),
			// since its association to 1 was not updated
			// (the code above is technically incorrect).
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void manyToOne_deleteNonOwner() {
		initManyToOne();

		withinTransaction( sessionFactory, session -> {
			AssociationOwner owner1 = session.getReference( AssociationOwner.class, 1 );
			AssociationOwner owner3 = session.getReference( AssociationOwner.class, 3 );
			AssociationNonOwner nonOwner2 = session.getReference( AssociationNonOwner.class, 2 );

			// Necessary because the foreign key will no longer reference an existing row.
			owner1.setManyToOne( null );
			owner3.setManyToOne( null );

			session.delete( nonOwner2 );

			backendMock.expectWorks( AssociationOwner.NAME )
					// This update is caused by the call to owner1.setManyToOne;
					// it has nothing to do with the deletion.
					.update( "1", b -> b.field( "basic", "text 1" )
							.field( "elementCollection", 1001, 2001 ) )
					// This update is caused by the call to owner3.setManyToOne;
					// it has nothing to do with the deletion.
					.update( "3", b -> b.field( "basic", "text 3" )
							.field( "elementCollection", 1003, 2003 ) )
					.processedThenExecuted();

			backendMock.expectWorks( AssociationNonOwner.NAME )
					.delete( "2" )
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void manyToOne_deleteBoth() {
		initManyToOne();

		withinTransaction( sessionFactory, session -> {
			AssociationOwner owner3 = session.getReference( AssociationOwner.class, 3 );
			AssociationOwner owner1 = session.getReference( AssociationOwner.class, 1 );
			AssociationNonOwner nonOwner2 = session.getReference( AssociationNonOwner.class, 2 );

			// Necessary because the foreign key will no longer reference an existing row.
			owner3.setManyToOne( null );

			session.delete( owner1 );
			session.delete( nonOwner2 );

			backendMock.expectWorks( AssociationOwner.NAME )
					// This update is caused by the call to owner3.setManyToOne;
					// it has nothing to do with the deletion.
					.update( "3", b -> b.field( "basic", "text 3" )
							.field( "elementCollection", 1003, 2003 ) )
					.delete( "1" )
					.processedThenExecuted();

			backendMock.expectWorks( AssociationNonOwner.NAME )
					.delete( "2" )
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	private void initManyToOne() {
		withinTransaction( sessionFactory, session -> {
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
									.field( "elementCollection", 1002, 2002 ) ) )
					.processedThenExecuted();
			backendMock.expectWorks( AssociationNonOwner.NAME )
					.add( "2", b -> b.field( "basic", "text 2" )
							.field( "elementCollection", 1002, 2002 )
							.objectField( "oneToMany", b2 -> b2
									.field( "basic", "text 1" )
									.field( "elementCollection", 1001, 2001 ) )
							.objectField( "oneToMany", b2 -> b2
									.field( "basic", "text 3" )
									.field( "elementCollection", 1003, 2003 ) ) )
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void manyToMany_deleteOwner() {
		initManyToMany();

		withinTransaction( sessionFactory, session -> {
			AssociationOwner owner1 = session.getReference( AssociationOwner.class, 1 );

			session.delete( owner1 );

			backendMock.expectWorks( AssociationOwner.NAME )
					.delete( "1" )
					.processedThenExecuted();

			// We don't expect any update of the containing entity (id 2),
			// since its association to 1 was not updated
			// (the code above is technically incorrect).
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void manyToMany_deleteNonOwner() {
		initManyToMany();

		withinTransaction( sessionFactory, session -> {
			AssociationOwner owner1 = session.getReference( AssociationOwner.class, 1 );
			AssociationOwner owner3 = session.getReference( AssociationOwner.class, 3 );
			AssociationNonOwner nonOwner2 = session.getReference( AssociationNonOwner.class, 2 );

			// Necessary because the foreign key will no longer reference an existing row.
			owner1.getManyToMany().remove( nonOwner2 );
			owner3.getManyToMany().remove( nonOwner2 );

			session.delete( nonOwner2 );

			backendMock.expectWorks( AssociationOwner.NAME )
					// This update is caused by the call to owner1.getManyToMany().remove();
					// it has nothing to do with the deletion.
					.update( "1", b -> b.field( "basic", "text 1" )
							.field( "elementCollection", 1001, 2001 )
							.objectField( "manyToMany", b2 -> b2
									.field( "basic", "text 4" )
									.field( "elementCollection", 1004, 2004 ) ) )
					// This update is caused by the call to owner3.getManyToMany().remove();
					// it has nothing to do with the deletion.
					.update( "3", b -> b.field( "basic", "text 3" )
							.field( "elementCollection", 1003, 2003 )
							.objectField( "manyToMany", b2 -> b2
									.field( "basic", "text 4" )
									.field( "elementCollection", 1004, 2004 ) ) )
					.processedThenExecuted();

			backendMock.expectWorks( AssociationNonOwner.NAME )
					.delete( "2" )
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void manyToMany_deleteBoth() {
		initManyToMany();

		withinTransaction( sessionFactory, session -> {
			AssociationOwner owner3 = session.getReference( AssociationOwner.class, 3 );
			AssociationOwner owner1 = session.getReference( AssociationOwner.class, 1 );
			AssociationNonOwner nonOwner2 = session.getReference( AssociationNonOwner.class, 2 );

			// Necessary because the foreign key will no longer reference an existing row.
			owner3.getManyToMany().remove( nonOwner2 );

			session.delete( owner1 );
			session.delete( nonOwner2 );

			backendMock.expectWorks( AssociationOwner.NAME )
					.delete( "1" )
					// This update is caused by the call to owner3.getManyToMany().remove;
					// it has nothing to do with the deletion.
					.update( "3", b -> b.field( "basic", "text 3" )
							.field( "elementCollection", 1003, 2003 )
							.objectField( "manyToMany", b2 -> b2
									.field( "basic", "text 4" )
									.field( "elementCollection", 1004, 2004 ) ) )
					.processedThenExecuted();

			backendMock.expectWorks( AssociationNonOwner.NAME )
					.delete( "2" )
					// We don't expect any update of the containing entity (id 4),
					// since its association to 1 was not updated
					// (the code above is technically incorrect).
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	private void initManyToMany() {
		withinTransaction( sessionFactory, session -> {
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
									.field( "elementCollection", 1004, 2004 ) ) )
					.processedThenExecuted();
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
									.field( "elementCollection", 1003, 2003 ) ) )
					.processedThenExecuted();
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

		@IndexedEmbedded(includePaths = {"basic", "elementCollection"})
		@OneToOne(fetch = FetchType.LAZY, optional = true)
		private AssociationNonOwner optionalOneToOne;

		@IndexedEmbedded(includePaths = {"basic", "elementCollection"})
		@ManyToOne
		private AssociationNonOwner manyToOne;

		@IndexedEmbedded(includePaths = {"basic", "elementCollection"})
		@ManyToMany
		@JoinTable(joinColumns = @JoinColumn(name = "nonowner_id"),
				inverseJoinColumns = @JoinColumn(name = "owner_id"))
		@OrderColumn
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

		@IndexedEmbedded(includePaths = {"basic", "elementCollection"})
		@OneToOne(fetch = FetchType.LAZY, // Will probably be ignored: an optional one-to-one can hardly use lazy proxies on the non-owning side.
				mappedBy = "optionalOneToOne", optional = true)
		private AssociationOwner optionalOneToOne;

		@IndexedEmbedded(includePaths = {"basic", "elementCollection"})
		@OneToMany(mappedBy = "manyToOne")
		@OrderColumn
		private List<AssociationOwner> oneToMany = new ArrayList<>();

		@IndexedEmbedded(includePaths = {"basic", "elementCollection"})
		@ManyToMany(mappedBy = "manyToMany")
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

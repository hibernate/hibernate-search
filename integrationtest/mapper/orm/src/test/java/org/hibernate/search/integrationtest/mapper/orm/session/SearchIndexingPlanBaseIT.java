/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.session;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.Session;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMapping;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

@TestForIssue(jiraKey = "HSEARCH-3049")
public class SearchIndexingPlanBaseIT {

	private static final String BACKEND2_NAME = "stubBackend2";

	@ClassRule
	public static BackendMock defaultBackendMock = new BackendMock();

	@ClassRule
	public static BackendMock backend2Mock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder;
	static {
		Map<String, BackendMock> namedBackendMocks = new LinkedHashMap<>();
		namedBackendMocks.put( BACKEND2_NAME, backend2Mock );
		setupHolder = ReusableOrmSetupHolder.withBackendMocks( defaultBackendMock, namedBackendMocks );
	}

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		defaultBackendMock.expectAnySchema( IndexedEntity1.INDEX_NAME );
		backend2Mock.expectAnySchema( IndexedEntity2.INDEX_NAME );

		setupContext.withAnnotatedTypes( IndexedEntity1.class, IndexedEntity2.class, ContainedEntity.class );
	}

	@After
	public void resetListenerEnabled() {
		listenerEnabled( true );
	}

	private void listenerEnabled(boolean enabled) {
		HibernateOrmMapping mapping = ( (HibernateOrmMapping) Search.mapping( setupHolder.sessionFactory() ) );
		mapping.listenerEnabled( enabled );
	}

	@Test
	public void simple() {
		listenerEnabled( false );

		setupHolder.runInTransaction( session -> {
			IndexedEntity1 entity1 = new IndexedEntity1( 1, "number1" );
			IndexedEntity1 entity2 = new IndexedEntity1( 2, "number2" );
			IndexedEntity1 entity3 = new IndexedEntity1( 3, "number3" );

			session.persist( entity1 );
			session.persist( entity2 );
			session.persist( entity3 );

			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();
			indexingPlan.addOrUpdate( entity1 );
			indexingPlan.addOrUpdate( entity2 );
			indexingPlan.delete( entity3 );
			indexingPlan.purge( IndexedEntity1.class, 42, null ); // Does not exist in database, but may exist in the index

			defaultBackendMock.expectWorks( IndexedEntity1.INDEX_NAME )
					.addOrUpdate( "1", b -> b.field( "text", "number1" ) )
					.addOrUpdate( "2", b -> b.field( "text", "number2" ) )
					.delete( "3" )
					.delete( "42" );
		} );
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	public void updateOnContainedEntityTriggersUpdateOfContaining() {
		listenerEnabled( false );

		setupHolder.runInTransaction( session -> {
			IndexedEntity1 entity1 = new IndexedEntity1( 1, "number1" );
			ContainedEntity contained1 = new ContainedEntity( 11, "text 1" );
			contained1.containing = entity1;
			entity1.contained = Arrays.asList( contained1 );

			IndexedEntity1 entity2 = new IndexedEntity1( 2, "number2" );
			ContainedEntity contained2 = new ContainedEntity( 12, "text 2" );
			contained2.containing = entity2;
			ContainedEntity contained3 = new ContainedEntity( 13, "text 3" );
			contained3.containing = entity2;
			entity2.contained = Arrays.asList( contained2, contained3 );

			IndexedEntity1 entity3 = new IndexedEntity1( 3, "number3" );

			session.persist( entity1 );
			session.persist( entity2 );
			session.persist( entity3 );
			session.persist( contained1 );
			session.persist( contained2 );
			session.persist( contained3 );

			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();
			indexingPlan.addOrUpdate( contained1 );
			indexingPlan.addOrUpdate( contained2 );

			defaultBackendMock.expectWorks( IndexedEntity1.INDEX_NAME )
					.addOrUpdate( "1", b -> b.field( "text", "number1" )
							.objectField( "contained", b2 -> b2.field( "text", "text 1" ) ) )
					.addOrUpdate( "2", b -> b.field( "text", "number2" )
							.objectField( "contained", b2 -> b2.field( "text", "text 2" ) )
							.objectField( "contained", b2 -> b2.field( "text", "text 3" ) )
					);
		} );

		setupHolder.runInTransaction( session -> {
			IndexedEntity1 entity1 = session.get( IndexedEntity1.class, 1 );
			ContainedEntity contained1 = entity1.contained.get( 0 );
			contained1.text = "new text 1";

			session.persist( contained1 );

			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();
			indexingPlan.addOrUpdate( contained1 );

			defaultBackendMock.expectWorks( IndexedEntity1.INDEX_NAME )
					.addOrUpdate( "1", b -> b.field( "text", "number1" )
							.objectField( "contained", b2 -> b2.field( "text", "new text 1" ) ) );
		} );
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	public void mergedEvents() {
		listenerEnabled( false );

		setupHolder.runInTransaction( session -> {
			IndexedEntity1 entity1 = new IndexedEntity1( 1, "number1" );
			IndexedEntity1 entity2 = new IndexedEntity1( 2, "number2" );
			IndexedEntity1 entity3 = new IndexedEntity1( 3, "number3" );
			IndexedEntity1 entity4 = new IndexedEntity1( 4, "number4" );
			IndexedEntity1 entity5 = new IndexedEntity1( 5, "number5" );
			IndexedEntity1 entity6 = new IndexedEntity1( 6, "number6" );
			IndexedEntity1 entity7 = new IndexedEntity1( 7, "number7" );
			IndexedEntity1 entity8 = new IndexedEntity1( 8, "number8" );

			session.persist( entity1 );
			session.persist( entity2 );
			session.persist( entity3 );
			session.persist( entity4 );
			session.persist( entity5 );
			session.persist( entity6 );
			session.persist( entity7 );
			session.persist( entity8 );

			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();

			indexingPlan.addOrUpdate( entity1 );
			indexingPlan.addOrUpdate( entity1 );

			indexingPlan.delete( entity2 );
			indexingPlan.delete( entity2 );

			indexingPlan.addOrUpdate( entity3 );
			indexingPlan.delete( entity3 );

			indexingPlan.delete( entity4 );
			indexingPlan.addOrUpdate( entity4 );

			indexingPlan.purge( IndexedEntity1.class, 42, null );
			indexingPlan.purge( IndexedEntity1.class, 42, null );

			indexingPlan.delete( entity5 );
			indexingPlan.purge( IndexedEntity1.class, 5, null );

			indexingPlan.purge( IndexedEntity1.class, 6, null );
			indexingPlan.delete( entity6 );

			indexingPlan.addOrUpdate( entity7 );
			indexingPlan.purge( IndexedEntity1.class, 7, null );

			indexingPlan.purge( IndexedEntity1.class, 8, null );
			indexingPlan.addOrUpdate( entity8 );

			defaultBackendMock.expectWorks( IndexedEntity1.INDEX_NAME )
					// multiple addOrUpdate => single update
					.addOrUpdate( "1", b -> b.field( "text", "number1" ) )
					// multiple delete => single delete
					.delete( "2" )
					// addOrUpdate then delete => delete
					.delete( "3" )
					// delete then addOrUpdate => update
					.addOrUpdate( "4", b -> b.field( "text", "number4" ) )
					// multiple purge => single delete
					.delete( "42" )
					// delete then purge => single delete
					.delete( "5" )
					// purge then delete => single delete
					.delete( "6" )
					// addOrUpdate then purge => delete
					.delete( "7" )
					// purge then addOrUpdate => update
					.addOrUpdate( "8", b -> b.field( "text", "number8" ) );
		} );
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	public void purgeByEntityClass_invalidClass() {
		listenerEnabled( false );

		Class<?> invalidClass = String.class;

		setupHolder.runInTransaction( session -> {
			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();
			assertThatThrownBy(
					() -> indexingPlan.purge( invalidClass, 42, null )
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "No matching entity type for class '" + invalidClass.getName() + "'",
							"Either this class is not an entity type, or the entity type is not mapped in Hibernate Search",
							"Valid classes for mapped entity types are: ["
									+ IndexedEntity1.class.getName() + ", "
									+ IndexedEntity2.class.getName() + ", "
									+ ContainedEntity.class.getName()
									+ "]" );
		} );
	}

	@Test
	public void purgeByEntityName() {
		listenerEnabled( false );

		setupHolder.runInTransaction( session -> {
			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();
			indexingPlan.purge( IndexedEntity1.NAME, 42, null ); // Does not exist in database, but may exist in the index

			defaultBackendMock.expectWorks( IndexedEntity1.INDEX_NAME )
					.delete( "42" );
		} );
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	public void purgeByEntityName_invalidName() {
		listenerEnabled( false );

		String invalidName = "foo";

		setupHolder.runInTransaction( session -> {
			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();
			assertThatThrownBy(
					() -> indexingPlan.purge( invalidName, 42, null )
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "No matching entity type for name '" + invalidName + "'",
							"Either this is not the name of an entity type, or the entity type is not mapped in Hibernate Search",
							"Valid names for mapped entity types are: ["
									// JPA entity names + Hibernate ORM entity names
									+ IndexedEntity1.NAME + ", "
									+ IndexedEntity1.class.getName() + ", "
									+ IndexedEntity2.NAME + ", "
									+ IndexedEntity2.class.getName() + ", "
									+ ContainedEntity.NAME + ", "
									+ ContainedEntity.class.getName()
									+ "]"
					);
		} );
	}

	@Test
	public void earlyProcess() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity1 entity1 = new IndexedEntity1( 1, "number1" );
			IndexedEntity1 entity2 = new IndexedEntity1( 2, "number2" );

			session.persist( entity1 );
			session.persist( entity2 );

			// flush triggers the prepare of the current indexing plan
			defaultBackendMock.expectWorks( IndexedEntity1.INDEX_NAME )
					.createFollowingWorks()
					.add( "1", b -> b.field( "text", "number1" ) )
					.add( "2", b -> b.field( "text", "number2" ) );

			session.flush();

			// Works should be prepared immediately
			defaultBackendMock.verifyExpectationsMet();

			/*
			 * Detach entities and change their data.
			 * This should not matter as the call to process() should have triggered reading.
			 * If it didn't, then data written to the index will be wrong and we'll detect it
			 * thanks to the expectations of the backend mock.
			 */
			session.detach( entity1 );
			session.detach( entity2 );
			entity1.text = "WRONG";
			entity2.text = "WRONG";

			defaultBackendMock.expectWorks( IndexedEntity1.INDEX_NAME )
					.executeFollowingWorks()
					.add( "1", b -> b.field( "text", "number1" ) )
					.add( "2", b -> b.field( "text", "number2" ) );
		} );
		// Works should be executed on transaction commit
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	public void earlyExecute() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity1 entity1 = new IndexedEntity1( 1, "number1" );
			IndexedEntity1 entity2 = new IndexedEntity1( 2, "number2" );

			session.persist( entity1 );
			session.persist( entity2 );

			defaultBackendMock.expectWorks( IndexedEntity1.INDEX_NAME )
					.createFollowingWorks()
					.add( "1", b -> b.field( "text", "number1" ) )
					.add( "2", b -> b.field( "text", "number2" ) );

			session.flush();
			defaultBackendMock.verifyExpectationsMet();

			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();

			defaultBackendMock.expectWorks( IndexedEntity1.INDEX_NAME )
					.executeFollowingWorks()
					.add( "1", b -> b.field( "text", "number1" ) )
					.add( "2", b -> b.field( "text", "number2" ) );

			indexingPlan.execute();

			// Works should be executed immediately
			defaultBackendMock.verifyExpectationsMet();
		} );
		// There shouldn't be any more work to execute
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	public void mixedExplicitAndAutomaticIndexing() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity1 entity1 = new IndexedEntity1( 1, "number1" );
			IndexedEntity1 entity2 = new IndexedEntity1( 2, "number2" );

			session.persist( entity1 );
			session.persist( entity2 );

			defaultBackendMock.expectWorks( IndexedEntity1.INDEX_NAME )
					.add( "1", b -> b.field( "text", "number1" ) )
					.add( "2", b -> b.field( "text", "number2" ) );
		} );
		defaultBackendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			IndexedEntity1 entity1 = session.getReference( IndexedEntity1.class, 1 );
			IndexedEntity1 entity2 = session.getReference( IndexedEntity1.class, 2 );
			IndexedEntity1 entity3 = new IndexedEntity1( 3, "number3" );

			session.persist( entity3 );

			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();
			indexingPlan.addOrUpdate( entity1 );
			indexingPlan.delete( entity2 );

			defaultBackendMock.expectWorks( IndexedEntity1.INDEX_NAME )
					// Requested explicitly
					.addOrUpdate( "1", b -> b.field( "text", "number1" ) )
					.delete( "2" )
					// Automatic on persist
					.add( "3", b -> b.field( "text", "number3" ) );
		} );
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	public void multiIndexMultiBackend() {
		listenerEnabled( false );

		setupHolder.runInTransaction( session -> {
			IndexedEntity1 entity1 = new IndexedEntity1( 1, "number1" );
			IndexedEntity2 entity2 = new IndexedEntity2( 2, "number2" );
			IndexedEntity1 entity3 = new IndexedEntity1( 3, "number3" );

			session.persist( entity1 );
			session.persist( entity2 );
			session.persist( entity3 );

			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();
			indexingPlan.addOrUpdate( entity1 );
			indexingPlan.addOrUpdate( entity2 );
			indexingPlan.delete( entity3 );

			defaultBackendMock.expectWorks( IndexedEntity1.INDEX_NAME )
					.addOrUpdate( "1", b -> b.field( "text", "number1" ) )
					.delete( "3" );
			backend2Mock.expectWorks( IndexedEntity2.INDEX_NAME )
					.addOrUpdate( "2", b -> b.field( "text", "number2" ) );
		} );
		defaultBackendMock.verifyExpectationsMet();
		backend2Mock.verifyExpectationsMet();
	}

	@Test
	public void outOfSession() {
		listenerEnabled( false );

		SearchIndexingPlan indexingPlan;
		IndexedEntity1 entity;
		try ( Session session = setupHolder.sessionFactory().openSession() ) {
			entity = new IndexedEntity1( 1, "number1" );
			session.persist( entity );
			indexingPlan = Search.session( session ).indexingPlan();
		}

		assertThatThrownBy(
				() -> indexingPlan.addOrUpdate( entity )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Underlying Hibernate ORM Session is closed" );

		assertThatThrownBy(
				() -> indexingPlan.delete( entity )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Underlying Hibernate ORM Session is closed" );

		assertThatThrownBy(
				() -> indexingPlan.process()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Underlying Hibernate ORM Session is closed" );

		assertThatThrownBy(
				() -> indexingPlan.execute()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Underlying Hibernate ORM Session is closed" );
	}

	@Entity(name = IndexedEntity1.NAME)
	@Indexed(index = IndexedEntity1.INDEX_NAME)
	public static class IndexedEntity1 {

		static final String NAME = "indexed1";

		static final String INDEX_NAME = "index1Name";

		@Id
		private Integer id;

		@GenericField
		private String text;

		@OneToMany(mappedBy = "containing")
		@IndexedEmbedded
		private List<ContainedEntity> contained;

		protected IndexedEntity1() {
			// For ORM
		}

		IndexedEntity1(int id, String text) {
			this.id = id;
			this.text = text;
		}
	}

	@Entity(name = IndexedEntity2.NAME)
	@Indexed(backend = BACKEND2_NAME, index = IndexedEntity2.INDEX_NAME)
	public static class IndexedEntity2 {

		static final String NAME = "indexed2";

		static final String INDEX_NAME = "index2Name";

		@Id
		private Integer id;

		@GenericField
		private String text;

		protected IndexedEntity2() {
			// For ORM
		}

		IndexedEntity2(int id, String text) {
			this.id = id;
			this.text = text;
		}
	}

	@Entity(name = ContainedEntity.NAME)
	public static class ContainedEntity {

		static final String NAME = "contained";

		@Id
		private Integer id;

		@GenericField
		private String text;

		@ManyToOne
		private IndexedEntity1 containing;

		protected ContainedEntity() {
			// For ORM
		}

		ContainedEntity(int id, String text) {
			this.id = id;
			this.text = text;
		}
	}
}

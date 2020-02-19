/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.session;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinTransaction;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-3049")
public class SearchIndexingPlanBaseIT {

	private static final String BACKEND1_NAME = "stubBackend1";
	private static final String BACKEND2_NAME = "stubBackend2";

	@Rule
	public BackendMock backend1Mock = new BackendMock( BACKEND1_NAME );

	@Rule
	public BackendMock backend2Mock = new BackendMock( BACKEND2_NAME );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMocks( backend1Mock, backend2Mock );

	@Test
	public void simple() {
		SessionFactory sessionFactory = setup( AutomaticIndexingStrategyName.NONE );

		withinTransaction( sessionFactory, session -> {
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

			backend1Mock.expectWorks( IndexedEntity1.INDEX_NAME )
					.update( "1", b -> b.field( "text", "number1" ) )
					.update( "2", b -> b.field( "text", "number2" ) )
					.delete( "3" )
					.delete( "42" )
					.processedThenExecuted();
		} );
		backend1Mock.verifyExpectationsMet();
	}

	@Test
	public void mergedEvents() {
		SessionFactory sessionFactory = setup( AutomaticIndexingStrategyName.NONE );

		withinTransaction( sessionFactory, session -> {
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

			backend1Mock.expectWorks( IndexedEntity1.INDEX_NAME )
					// multiple addOrUpdate => single update
					.update( "1", b -> b.field( "text", "number1" ) )
					// multiple delete => single delete
					.delete( "2" )
					// addOrUpdate then delete => delete
					.delete( "3" )
					// delete then addOrUpdate => update
					.update( "4", b -> b.field( "text", "number4" ) )
					// multiple purge => single delete
					.delete( "42" )
					// delete then purge => single delete
					.delete( "5" )
					// purge then delete => single delete
					.delete( "6" )
					// addOrUpdate then purge => delete
					.delete( "7" )
					// purge then addOrUpdate => update
					.update( "8", b -> b.field( "text", "number8" ) )
					.processedThenExecuted();
		} );
		backend1Mock.verifyExpectationsMet();
	}

	@Test
	public void purgeByEntityClass_invalidClass() {
		SessionFactory sessionFactory = setup( AutomaticIndexingStrategyName.NONE );

		Class<?> invalidClass = String.class;

		withinTransaction( sessionFactory, session -> {
			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();
			SubTest.expectException(
					() -> indexingPlan.purge( invalidClass, 42, null )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"Cannot work on type '" + invalidClass.getName() + "', because it is not indexed,"
									+ " neither directly nor as a contained entity in another type"
					);
		} );
	}

	@Test
	public void purgeByEntityClass_invalidClass_contained() {
		SessionFactory sessionFactory = setup( AutomaticIndexingStrategyName.NONE );

		Class<?> invalidClass = ContainedEntity.class;

		withinTransaction( sessionFactory, session -> {
			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();
			SubTest.expectException(
					() -> indexingPlan.purge( invalidClass, 42, null )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"Type '" + ContainedEntity.class.getName() + "' is contained in an indexed type but is not itself indexed",
							"thus entity with identifier '42' cannot be purged"
					);
		} );
	}

	@Test
	public void purgeByEntityName() {
		SessionFactory sessionFactory = setup( AutomaticIndexingStrategyName.NONE );

		withinTransaction( sessionFactory, session -> {
			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();
			indexingPlan.purge( IndexedEntity1.NAME, 42, null ); // Does not exist in database, but may exist in the index

			backend1Mock.expectWorks( IndexedEntity1.INDEX_NAME )
					.delete( "42" )
					.processedThenExecuted();
		} );
		backend1Mock.verifyExpectationsMet();
	}

	@Test
	public void purgeByEntityName_invalidName() {
		SessionFactory sessionFactory = setup( AutomaticIndexingStrategyName.NONE );

		String invalidName = "foo";

		withinTransaction( sessionFactory, session -> {
			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();
			SubTest.expectException(
					() -> indexingPlan.purge( invalidName, 42, null )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"Unknown entity name: '" + invalidName + "'",
							"Available entity names: ["
									// Hibernate ORM entity names
									+ ContainedEntity.class.getName() + ", "
									+ IndexedEntity1.class.getName() + ", "
									+ IndexedEntity2.class.getName() + ", "
									// JPA entity names
									+ ContainedEntity.NAME + ", "
									+ IndexedEntity1.NAME + ", "
									+ IndexedEntity2.NAME
									+ "]"
					);
		} );
	}

	@Test
	public void earlyProcess() {
		SessionFactory sessionFactory = setup( AutomaticIndexingStrategyName.SESSION );

		withinTransaction( sessionFactory, session -> {
			IndexedEntity1 entity1 = new IndexedEntity1( 1, "number1" );
			IndexedEntity1 entity2 = new IndexedEntity1( 2, "number2" );

			session.persist( entity1 );
			session.persist( entity2 );

			// flush triggers the prepare of the current indexing plan
			backend1Mock.expectWorks( IndexedEntity1.INDEX_NAME )
					.add( "1", b -> b.field( "text", "number1" ) )
					.add( "2", b -> b.field( "text", "number2" ) )
					.processed();

			session.flush();

			// Works should be prepared immediately
			backend1Mock.verifyExpectationsMet();

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

			backend1Mock.expectWorks( IndexedEntity1.INDEX_NAME )
					.add( "1", b -> b.field( "text", "number1" ) )
					.add( "2", b -> b.field( "text", "number2" ) )
					.executed();
		} );
		// Works should be executed on transaction commit
		backend1Mock.verifyExpectationsMet();
	}

	@Test
	public void earlyExecute() {
		SessionFactory sessionFactory = setup( AutomaticIndexingStrategyName.SESSION );

		withinTransaction( sessionFactory, session -> {
			IndexedEntity1 entity1 = new IndexedEntity1( 1, "number1" );
			IndexedEntity1 entity2 = new IndexedEntity1( 2, "number2" );

			session.persist( entity1 );
			session.persist( entity2 );

			backend1Mock.expectWorks( IndexedEntity1.INDEX_NAME )
					.add( "1", b -> b.field( "text", "number1" ) )
					.add( "2", b -> b.field( "text", "number2" ) )
					.processed();

			session.flush();
			backend1Mock.verifyExpectationsMet();

			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();

			backend1Mock.expectWorks( IndexedEntity1.INDEX_NAME )
					.add( "1", b -> b.field( "text", "number1" ) )
					.add( "2", b -> b.field( "text", "number2" ) )
					.executed();

			indexingPlan.execute();

			// Works should be executed immediately
			backend1Mock.verifyExpectationsMet();
		} );
		// There shouldn't be any more work to execute
		backend1Mock.verifyExpectationsMet();
	}

	@Test
	public void mixedExplicitAndAutomaticIndexing() {
		SessionFactory sessionFactory = setup( AutomaticIndexingStrategyName.SESSION );

		withinTransaction( sessionFactory, session -> {
			IndexedEntity1 entity1 = new IndexedEntity1( 1, "number1" );
			IndexedEntity1 entity2 = new IndexedEntity1( 2, "number2" );

			session.persist( entity1 );
			session.persist( entity2 );

			backend1Mock.expectWorks( IndexedEntity1.INDEX_NAME )
					.add( "1", b -> b.field( "text", "number1" ) )
					.add( "2", b -> b.field( "text", "number2" ) )
					.processedThenExecuted();
		} );
		backend1Mock.verifyExpectationsMet();

		withinTransaction( sessionFactory, session -> {
			IndexedEntity1 entity1 = session.getReference( IndexedEntity1.class, 1 );
			IndexedEntity1 entity2 = session.getReference( IndexedEntity1.class, 2 );
			IndexedEntity1 entity3 = new IndexedEntity1( 3, "number3" );

			session.persist( entity3 );

			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();
			indexingPlan.addOrUpdate( entity1 );
			indexingPlan.delete( entity2 );

			backend1Mock.expectWorks( IndexedEntity1.INDEX_NAME )
					// Requested explicitly
					.update( "1", b -> b.field( "text", "number1" ) )
					.delete( "2" )
					// Automatic on persist
					.add( "3", b -> b.field( "text", "number3" ) )
					.processedThenExecuted();
		} );
		backend1Mock.verifyExpectationsMet();
	}

	@Test
	public void multiIndexMultiBackend() {
		SessionFactory sessionFactory = setup( AutomaticIndexingStrategyName.NONE );

		withinTransaction( sessionFactory, session -> {
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

			backend1Mock.expectWorks( IndexedEntity1.INDEX_NAME )
					.update( "1", b -> b.field( "text", "number1" ) )
					.delete( "3" )
					.processedThenExecuted();
			backend2Mock.expectWorks( IndexedEntity2.INDEX_NAME )
					.update( "2", b -> b.field( "text", "number2" ) )
					.processedThenExecuted();
		} );
		backend1Mock.verifyExpectationsMet();
		backend2Mock.verifyExpectationsMet();
	}

	@Test
	public void outOfSession() {
		SessionFactory sessionFactory = setup( AutomaticIndexingStrategyName.NONE );

		SearchIndexingPlan indexingPlan;
		IndexedEntity1 entity;
		try ( Session session = sessionFactory.openSession() ) {
			entity = new IndexedEntity1( 1, "number1" );
			session.persist( entity );
			indexingPlan = Search.session( session ).indexingPlan();
		}

		SubTest.expectException(
				() -> indexingPlan.addOrUpdate( entity )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Underlying Hibernate ORM Session seems to be closed" );

		SubTest.expectException(
				() -> indexingPlan.delete( entity )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Underlying Hibernate ORM Session seems to be closed" );

		SubTest.expectException(
				() -> indexingPlan.process()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Underlying Hibernate ORM Session seems to be closed" );

		SubTest.expectException(
				() -> indexingPlan.execute()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Underlying Hibernate ORM Session seems to be closed" );
	}

	private SessionFactory setup(AutomaticIndexingStrategyName automaticIndexingStrategy) {
		backend1Mock.expectAnySchema( IndexedEntity1.INDEX_NAME );
		backend2Mock.expectAnySchema( IndexedEntity2.INDEX_NAME );

		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_STRATEGY,
						automaticIndexingStrategy
				)
				.setup( IndexedEntity1.class, IndexedEntity2.class, ContainedEntity.class );

		backend1Mock.verifyExpectationsMet();
		backend2Mock.verifyExpectationsMet();

		return sessionFactory;
	}

	@Entity(name = IndexedEntity1.NAME)
	@Indexed(backend = BACKEND1_NAME, index = IndexedEntity1.INDEX_NAME)
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

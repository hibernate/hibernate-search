/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import static org.hibernate.search.util.impl.integrationtest.orm.OrmUtils.withinTransaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import javax.persistence.Basic;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSessionWritePlan;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test automatic indexing based on Hibernate ORM entity events.
 *
 * This test only checks basic, direct updates to the entity state.
 * Other tests in the same package check more complex updates involving associations.
 */
public class AutomaticIndexingBasicIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "indexedField", String.class )
				.field( "noReindexOnUpdateField", String.class )
				.field( "indexedElementCollectionField", String.class, b2 -> b2.multiValued( true ) )
				.field( "noReindexOnUpdateElementCollectionField", String.class, b2 -> b2.multiValued( true ) )
		);

		sessionFactory = ormSetupHelper.start()
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void directPersistUpdateDelete() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "initialValue" );
			entity1.getIndexedElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", entity1.getIndexedField() )
							.field( "noReindexOnUpdateField", null )
							.field(
									"indexedElementCollectionField",
									entity1.getIndexedElementCollectionField().get( 0 )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setIndexedField( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "indexedField", entity1.getIndexedField() )
							.field( "noReindexOnUpdateField", null )
							.field(
									"indexedElementCollectionField",
									entity1.getIndexedElementCollectionField().get( 0 )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			session.delete( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.delete( "1" )
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void rollback_discardPreparedWorks() {
		OrmUtils.withinSession( sessionFactory, session -> {
			Transaction trx = session.beginTransaction();
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "initialValue" );
			entity1.getIndexedElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", entity1.getIndexedField() )
							.field( "noReindexOnUpdateField", null )
							.field(
									"indexedElementCollectionField",
									entity1.getIndexedElementCollectionField().get( 0 )
							)
					)
					.prepared();

			session.flush();
			backendMock.verifyExpectationsMet();

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", entity1.getIndexedField() )
							.field( "noReindexOnUpdateField", null )
							.field(
									"indexedElementCollectionField",
									entity1.getIndexedElementCollectionField().get( 0 )
							)
					)
					.discarded();

			trx.rollback();
			backendMock.verifyExpectationsMet();
		} );
	}

	@Test
	public void directValueUpdate_indexedElementCollectionField() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.getIndexedElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", null )
							.field( "noReindexOnUpdateField", null )
							.field(
									"indexedElementCollectionField",
									entity1.getIndexedElementCollectionField().get( 0 )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getIndexedElementCollectionField().add( "secondValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "indexedField", null )
							.field( "noReindexOnUpdateField", null )
							.field(
									"indexedElementCollectionField",
									entity1.getIndexedElementCollectionField().get( 0 ),
									entity1.getIndexedElementCollectionField().get( 1 )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getIndexedElementCollectionField().remove( 1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "indexedField", null )
							.field( "noReindexOnUpdateField", null )
							.field(
									"indexedElementCollectionField",
									entity1.getIndexedElementCollectionField().get( 0 )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing an indexed element collection
	 * does trigger reindexing of the indexed entity owning the collection.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3199")
	public void directValueReplace_indexedElementCollectionField() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.getIndexedElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", null )
							.field( "noReindexOnUpdateField", null )
							.field(
									"indexedElementCollectionField",
									entity1.getIndexedElementCollectionField().get( 0 )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setIndexedElementCollectionField( new ArrayList<>( Arrays.asList(
					"newFirstValue", "newSecondValue"
			) ) );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "indexedField", null )
							.field( "noReindexOnUpdateField", null )
							.field(
									"indexedElementCollectionField",
									entity1.getIndexedElementCollectionField().get( 0 ),
									entity1.getIndexedElementCollectionField().get( 1 )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that updating a non-indexed basic property
	 * does not trigger reindexing of the indexed entity owning the property.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3199")
	public void directValueUpdate_nonIndexedField() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setNonIndexedField( "initialValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", entity1.getIndexedField() )
							.field( "noReindexOnUpdateField", null )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setNonIndexedField( "updatedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setNonIndexedField( null );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that updating a non-indexed element collection
	 * does not trigger reindexing of the indexed entity owning the collection.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3199")
	public void directValueUpdate_nonIndexedElementCollectionField() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.getNonIndexedElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", null )
							.field( "noReindexOnUpdateField", null )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getNonIndexedElementCollectionField().add( "secondValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getNonIndexedElementCollectionField().remove( 1 );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing a non-indexed element collection
	 * does not trigger reindexing of the indexed entity owning the collection.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3204")
	public void directValueReplace_nonIndexedElementCollectionField() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.getNonIndexedElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", null )
							.field( "noReindexOnUpdateField", null )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setNonIndexedElementCollectionField( new ArrayList<>( Arrays.asList(
					"newFirstValue", "newSecondValue"
			) ) );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "indexedField", null )
							.field( "noReindexOnUpdateField", null )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that updating a indexed basic property configured with reindexOnUpdate = NO
	 * does not trigger reindexing of the indexed entity owning the property.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3206")
	public void directValueUpdate_noReindexOnUpdateField() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setNoReindexOnUpdateField( "initialValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", null )
							.field( "noReindexOnUpdateField", entity1.getNoReindexOnUpdateField() )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setNoReindexOnUpdateField( "updatedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setNoReindexOnUpdateField( null );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that updating an indexed element collection configured with reindexOnUpdate = NO
	 * does not trigger reindexing of the indexed entity owning the collection.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3206")
	public void directValueUpdate_noReindexOnUpdateElementCollectionField() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.getNoReindexOnUpdateElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", null )
							.field( "noReindexOnUpdateField", null )
							.field( "noReindexOnUpdateElementCollectionField", "firstValue" )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getNoReindexOnUpdateElementCollectionField().add( "secondValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getNoReindexOnUpdateElementCollectionField().remove( 1 );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing an indexed element collection configured with reindexOnUpdate = NO
	 * does not trigger reindexing of the indexed entity owning the collection.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3204")
	public void directValueReplace_noReindexOnUpdateElementCollectionField() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.getNoReindexOnUpdateElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", null )
							.field( "noReindexOnUpdateField", null )
							.field( "noReindexOnUpdateElementCollectionField", "firstValue" )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setNoReindexOnUpdateElementCollectionField( new ArrayList<>( Arrays.asList(
					"newFirstValue", "newSecondValue"
			) ) );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "indexedField", null )
							.field( "noReindexOnUpdateField", null )
							.field(
									"noReindexOnUpdateElementCollectionField",
									"newFirstValue", "newSecondValue"
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void sessionClear() {
		withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "number1" );
			IndexedEntity entity2 = new IndexedEntity( 2, "number2" );

			session.persist( entity1 );
			session.persist( entity2 );

			// flush triggers the prepare of the current PojoWorkPlan
			Consumer<StubDocumentNode.Builder> documentFieldConsumer = b -> b.field( "indexedField", "number1" ).field( "noReindexOnUpdateField", null );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", expectedValue( "number1" ) )
					.add( "2", expectedValue( "number2" ) )
					.prepared();

			session.flush();

			// Works should be prepared immediately
			backendMock.verifyExpectationsMet();

			IndexedEntity entity3 = new IndexedEntity( 3, "number3" );
			IndexedEntity entity4 = new IndexedEntity( 4, "number4" );

			session.persist( entity3 );
			session.persist( entity4 );

			// without clear the session
			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "3", expectedValue( "number3" ) )
					.add( "4", expectedValue( "number4" ) )
					.prepared();

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", expectedValue( "number1" ) )
					.add( "2", expectedValue( "number2" ) )
					.add( "3", expectedValue( "number3" ) )
					.add( "4", expectedValue( "number4" ) )
					.executed();
		} );
		// Works should be executed on transaction commit
		backendMock.verifyExpectationsMet();

		withinTransaction( sessionFactory, session -> {
			IndexedEntity entity5 = new IndexedEntity( 5, "number5" );
			IndexedEntity entity6 = new IndexedEntity( 6, "number6" );

			session.persist( entity5 );
			session.persist( entity6 );

			// flush triggers the prepare of the current PojoWorkPlan
			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "5", expectedValue( "number5" ) )
					.add( "6", expectedValue( "number6" ) )
					.prepared();

			session.flush();

			// Works should be prepared immediately
			backendMock.verifyExpectationsMet();

			IndexedEntity entity7 = new IndexedEntity( 7, "number7" );
			IndexedEntity entity8 = new IndexedEntity( 8, "number8" );
			IndexedEntity entity9 = new IndexedEntity( 9, "number9" );
			IndexedEntity entity10 = new IndexedEntity( 10, "number10" );

			session.persist( entity7 );
			session.persist( entity8 );

			SearchSessionWritePlan writePlan = Search.session( session ).writePlan();
			writePlan.addOrUpdate( entity9 );
			writePlan.addOrUpdate( entity10 );

			// the clear will revert the changes that haven't been flushed yet,
			// including the ones that have been inserted directly in the write plan (bypassing the ORM session)
			session.clear();

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "5", expectedValue( "number5" ) )
					.add( "6", expectedValue( "number6" ) )
					.executed();
		} );
		// Works should be executed on transaction commit
		backendMock.verifyExpectationsMet();
	}

	public Consumer<StubDocumentNode.Builder> expectedValue(String indexedFieldExpectedValue) {
		return b -> b.field( "indexedField", indexedFieldExpectedValue ).field( "noReindexOnUpdateField", null );
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {

		static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@Basic
		@GenericField
		private String indexedField;

		@ElementCollection
		@GenericField
		private List<String> indexedElementCollectionField = new ArrayList<>();

		@Basic
		private String nonIndexedField;

		@ElementCollection
		private List<String> nonIndexedElementCollectionField = new ArrayList<>();

		@Basic
		@GenericField
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)
		private String noReindexOnUpdateField;

		@ElementCollection
		@GenericField
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)
		private List<String> noReindexOnUpdateElementCollectionField = new ArrayList<>();

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id, String indexedField) {
			this.id = id;
			this.indexedField = indexedField;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}

		public String getNoReindexOnUpdateField() {
			return noReindexOnUpdateField;
		}

		public void setNoReindexOnUpdateField(String noReindexOnUpdateField) {
			this.noReindexOnUpdateField = noReindexOnUpdateField;
		}

		public List<String> getIndexedElementCollectionField() {
			return indexedElementCollectionField;
		}

		public void setIndexedElementCollectionField(List<String> indexedElementCollectionField) {
			this.indexedElementCollectionField = indexedElementCollectionField;
		}

		public String getNonIndexedField() {
			return nonIndexedField;
		}

		public void setNonIndexedField(String nonIndexedField) {
			this.nonIndexedField = nonIndexedField;
		}

		public List<String> getNonIndexedElementCollectionField() {
			return nonIndexedElementCollectionField;
		}

		public void setNonIndexedElementCollectionField(List<String> nonIndexedElementCollectionField) {
			this.nonIndexedElementCollectionField = nonIndexedElementCollectionField;
		}

		public List<String> getNoReindexOnUpdateElementCollectionField() {
			return noReindexOnUpdateElementCollectionField;
		}

		public void setNoReindexOnUpdateElementCollectionField(List<String> noReindexOnUpdateElementCollectionField) {
			this.noReindexOnUpdateElementCollectionField = noReindexOnUpdateElementCollectionField;
		}
	}

}

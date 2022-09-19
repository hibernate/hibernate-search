/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import static org.junit.Assume.assumeTrue;

import java.util.function.Consumer;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Transaction;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

/**
 * Test automatic indexing based on Hibernate ORM entity events for basic fields.
 *
 * This test only checks basic, direct updates to the entity state.
 * Other tests in the same package check more complex updates involving associations.
 */
public class AutomaticIndexingBasicIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "indexedField", String.class )
				.field( "shallowReindexOnUpdateField", String.class )
				.field( "noReindexOnUpdateField", String.class )
		);

		setupContext.withAnnotatedTypes( IndexedEntity.class );
	}

	@Test
	public void directPersistUpdateDelete() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "initialValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", entity1.getIndexedField() )
							.field( "shallowReindexOnUpdateField", null )
							.field( "noReindexOnUpdateField", null )
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setIndexedField( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.field( "indexedField", entity1.getIndexedField() )
							.field( "shallowReindexOnUpdateField", null )
							.field( "noReindexOnUpdateField", null )
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			session.remove( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.delete( "1" );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void rollback_discardPreparedWorks() {
		assumeTrue( "This test only makes sense if entities are processed in-session",
				setupHolder.areEntitiesProcessedInSession() );

		setupHolder.runNoTransaction( session -> {
			Transaction trx = session.beginTransaction();
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "initialValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.createFollowingWorks()
					.add( "1", b -> b
							.field( "indexedField", entity1.getIndexedField() )
							.field( "shallowReindexOnUpdateField", null )
							.field( "noReindexOnUpdateField", null )
					);

			session.flush();
			// Entities should be processed and works created on flush
			backendMock.verifyExpectationsMet();

			backendMock.expectWorks( IndexedEntity.INDEX )
					.discardFollowingWorks()
					.add( "1", b -> b
							.field( "indexedField", entity1.getIndexedField() )
							.field( "shallowReindexOnUpdateField", null )
							.field( "noReindexOnUpdateField", null )
					);

			trx.rollback();
			backendMock.verifyExpectationsMet();
		} );
	}

	/**
	 * Test that updating a non-indexed basic property
	 * does not trigger reindexing of the indexed entity owning the property.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3199")
	public void directValueUpdate_nonIndexedField() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setNonIndexedField( "initialValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", entity1.getIndexedField() )
							.field( "shallowReindexOnUpdateField", null )
							.field( "noReindexOnUpdateField", null )
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setNonIndexedField( "updatedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setNonIndexedField( null );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that updating a indexed basic property configured with reindexOnUpdate = SHALLOW
	 * does trigger reindexing of the indexed entity owning the property.
	 * <p>
	 * SHALLOW isn't really useful in this case, since there's no "depth" to speak of,
	 * but we're testing this anyway, for the sake of completeness.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4001")
	public void directValueUpdate_shallowReindexOnUpdateField() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setShallowReindexOnUpdateField( "initialValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", null )
							.field( "shallowReindexOnUpdateField", entity1.getShallowReindexOnUpdateField() )
							.field( "noReindexOnUpdateField", null )
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setShallowReindexOnUpdateField( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.field( "indexedField", null )
							.field( "shallowReindexOnUpdateField", entity1.getShallowReindexOnUpdateField() )
							.field( "noReindexOnUpdateField", null )
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setShallowReindexOnUpdateField( null );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.field( "indexedField", null )
							.field( "shallowReindexOnUpdateField", null )
							.field( "noReindexOnUpdateField", null )
					);
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
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setNoReindexOnUpdateField( "initialValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", null )
							.field( "shallowReindexOnUpdateField", null )
							.field( "noReindexOnUpdateField", entity1.getNoReindexOnUpdateField() )
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setNoReindexOnUpdateField( "updatedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setNoReindexOnUpdateField( null );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void sessionClear() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "number1" );
			IndexedEntity entity2 = new IndexedEntity( 2, "number2" );

			session.persist( entity1 );
			session.persist( entity2 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.createFollowingWorks()
					.add( "1", expectedValue( "number1" ) )
					.add( "2", expectedValue( "number2" ) );

			session.flush();
			if ( setupHolder.areEntitiesProcessedInSession() ) {
				// Entities should be processed and works created on flush
				backendMock.verifyExpectationsMet();
			}

			IndexedEntity entity3 = new IndexedEntity( 3, "number3" );
			IndexedEntity entity4 = new IndexedEntity( 4, "number4" );

			session.persist( entity3 );
			session.persist( entity4 );

			// without clear the session
			backendMock.expectWorks( IndexedEntity.INDEX )
					.createFollowingWorks()
					.add( "3", expectedValue( "number3" ) )
					.add( "4", expectedValue( "number4" ) );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.executeFollowingWorks()
					.add( "1", expectedValue( "number1" ) )
					.add( "2", expectedValue( "number2" ) )
					.add( "3", expectedValue( "number3" ) )
					.add( "4", expectedValue( "number4" ) );
		} );
		// Works should be executed on transaction commit
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			IndexedEntity entity5 = new IndexedEntity( 5, "number5" );
			IndexedEntity entity6 = new IndexedEntity( 6, "number6" );

			session.persist( entity5 );
			session.persist( entity6 );

			// flush triggers the prepare of the current indexing plan
			backendMock.expectWorks( IndexedEntity.INDEX )
					.createFollowingWorks()
					.add( "5", expectedValue( "number5" ) )
					.add( "6", expectedValue( "number6" ) );

			session.flush();
			if ( setupHolder.areEntitiesProcessedInSession() ) {
				// Entities should be processed and works created on flush
				backendMock.verifyExpectationsMet();
			}

			IndexedEntity entity7 = new IndexedEntity( 7, "number7" );
			IndexedEntity entity8 = new IndexedEntity( 8, "number8" );
			IndexedEntity entity9 = new IndexedEntity( 9, "number9" );
			IndexedEntity entity10 = new IndexedEntity( 10, "number10" );

			session.persist( entity7 );
			session.persist( entity8 );

			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();
			indexingPlan.addOrUpdate( entity9 );
			indexingPlan.addOrUpdate( entity10 );

			// the clear will revert the changes that haven't been flushed yet,
			// including the ones that have been inserted directly in the indexing plan (bypassing the ORM session)
			session.clear();

			backendMock.expectWorks( IndexedEntity.INDEX )
					.executeFollowingWorks()
					.add( "5", expectedValue( "number5" ) )
					.add( "6", expectedValue( "number6" ) );
		} );
		// Works should be executed on transaction commit
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that merging an entity using update() to change an indexed field
	 * triggers reindexing of the indexed entity owning the property.
	 */
	@Test
	@SuppressWarnings("deprecation") // This is specifically about "update", which is NOT strictly equivalent to "merge"
	public void sessionUpdate_directValueUpdate_indexedField() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "initialValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", entity1.getIndexedField() )
							.field( "shallowReindexOnUpdateField", null )
							.field( "noReindexOnUpdateField", null )
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "updatedValue" );

			session.update( entity1 );

			// Hibernate ORM does not track dirtiness on calls to update(): we assume everything is dirty.
			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.field( "indexedField", entity1.getIndexedField() )
							.field( "shallowReindexOnUpdateField", null )
							.field( "noReindexOnUpdateField", null )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that merging an entity using update() to change a non-indexed field
	 * triggers reindexing of the indexed entity owning the property:
	 */
	@SuppressWarnings("deprecation") // This is specifically about "update", which is NOT strictly equivalent to "merge"
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3199")
	public void sessionUpdate_directValueUpdate_nonIndexedField() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setNonIndexedField( "initialValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", entity1.getIndexedField() )
							.field( "shallowReindexOnUpdateField", null )
							.field( "noReindexOnUpdateField", null )
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setNonIndexedField( "updatedValue" );

			session.update( entity1 );

			// Hibernate ORM does not track dirtiness on calls to update(): we assume everything is dirty.
			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.field( "indexedField", entity1.getIndexedField() )
							.field( "shallowReindexOnUpdateField", null )
							.field( "noReindexOnUpdateField", null )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that merging an entity using merge() to change an indexed field
	 * triggers reindexing of the indexed entity owning the property.
	 */
	@Test
	public void sessionMerge_directValueUpdate_indexedField() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "initialValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", entity1.getIndexedField() )
							.field( "shallowReindexOnUpdateField", null )
							.field( "noReindexOnUpdateField", null )
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "updatedValue" );

			session.merge( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.field( "indexedField", entity1.getIndexedField() )
							.field( "shallowReindexOnUpdateField", null )
							.field( "noReindexOnUpdateField", null )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that merging an entity using merge() to change a non-indexed field
	 * does not trigger reindexing of the indexed entity owning the property.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3199")
	public void sessionMerge_directValueUpdate_nonIndexedField() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setNonIndexedField( "initialValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", entity1.getIndexedField() )
							.field( "shallowReindexOnUpdateField", null )
							.field( "noReindexOnUpdateField", null )
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setNonIndexedField( "updatedValue" );

			session.merge( entity1 );
		} );
		backendMock.verifyExpectationsMet();
	}

	public Consumer<StubDocumentNode.Builder> expectedValue(String indexedFieldExpectedValue) {
		return b -> b.field( "indexedField", indexedFieldExpectedValue )
				.field( "shallowReindexOnUpdateField", null )
				.field( "noReindexOnUpdateField", null );
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

		@Basic
		private String nonIndexedField;

		@Basic
		@GenericField
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
		private String shallowReindexOnUpdateField;

		@Basic
		@GenericField
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)
		private String noReindexOnUpdateField;

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

		public String getNonIndexedField() {
			return nonIndexedField;
		}

		public void setNonIndexedField(String nonIndexedField) {
			this.nonIndexedField = nonIndexedField;
		}

		public String getShallowReindexOnUpdateField() {
			return shallowReindexOnUpdateField;
		}

		public void setShallowReindexOnUpdateField(String shallowReindexOnUpdateField) {
			this.shallowReindexOnUpdateField = shallowReindexOnUpdateField;
		}

		public String getNoReindexOnUpdateField() {
			return noReindexOnUpdateField;
		}

		public void setNoReindexOnUpdateField(String noReindexOnUpdateField) {
			this.noReindexOnUpdateField = noReindexOnUpdateField;
		}

	}

}

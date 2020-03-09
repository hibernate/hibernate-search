/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class AutomaticIndexingConcurrentModificationInSameTypeIT {
	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "firstName", String.class )
				.field( "name", String.class )
				.objectField( "child", b2 -> b2
						.field( "firstName", String.class )
						.field( "name", String.class )
				)
		);

		sessionFactory = ormSetupHelper.start().withProperty(
				HibernateOrmMapperSettings.AUTOMATIC_INDEXING_STRATEGY,
				AutomaticIndexingStrategyName.NONE
		).setup( IndexedEntity.class );

		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3857")
	public void updateTriggeringReindexingOfPreviouslyUnknownEntity() {
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

		// First transaction
		OrmUtils.withinTransaction( sessionFactory, session -> {
			session.persist( entity1 );
			session.persist( entity2 );
			session.persist( entity3 );
			session.flush();
			backendMock.verifyExpectationsMet();
		} );

		// Second transaction
		OrmUtils.withinTransaction( sessionFactory, session -> {
			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();
			indexingPlan.addOrUpdate( session.load( IndexedEntity.class, 1 ) );
			// Add another entity to the indexing plan so that we're not done iterating over all entities
			// when entity2 is added to the indexing plan due to the change in the child.
			indexingPlan.addOrUpdate( session.load( IndexedEntity.class, 3 ) );

			backendMock.expectWorksAnyOrder(
					IndexedEntity.INDEX, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE )
					.update( entity1.getId().toString(), b -> b
							.field( "name", entity1.getName() )
							.field( "firstName", entity1.getFirstName() )
					)
					.update( entity2.getId().toString(), b -> b
							.field( "name", entity2.getName() )
							.field( "firstName", entity2.getFirstName() )
							.objectField( "child", b2 -> b2
									.field( "firstName", entity1.getFirstName() )
									.field( "name", entity1.getName() )
							)
					)
					.update( entity3.getId().toString(), b -> b
							.field( "name", entity3.getName() )
							.field( "firstName", entity3.getFirstName() )
					)
					.processedThenExecuted();
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

		@IndexedEmbedded(maxDepth = 1)
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

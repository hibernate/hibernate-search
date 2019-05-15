/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.nonregression.massindexing;

import static org.assertj.core.api.Fail.fail;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.index.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.index.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmAutomaticIndexingStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-3529")
public class PrimitiveIdMassIndexingIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = new OrmSetupHelper();

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectAnySchema( EntityWithPrimitiveId.INDEX );

		sessionFactory = ormSetupHelper.withBackendMock( backendMock )
				.withPropertyRadical( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_STRATEGY, HibernateOrmAutomaticIndexingStrategyName.NONE )
				.setup( EntityWithPrimitiveId.class );

		backendMock.verifyExpectationsMet();

		initData();
	}

	@Test
	public void entityWithPrimitiveId() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.getSearchSession( session );
			MassIndexer indexer = searchSession.createIndexer();

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorksAnyOrder(
					EntityWithPrimitiveId.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "1", b -> { } )
					.add( "2", b -> { } )
					.add( "3", b -> { } )
					.preparedThenExecuted();

			// purgeAtStart, optimizeAfterPurge and purgeAtStart flags are active by default,
			// so we expect 1 purge, 2 optimize and 1 flush calls in this order:
			backendMock.expectWorks( EntityWithPrimitiveId.INDEX )
					.purge( session.getTenantIdentifier() )
					.optimize()
					.optimize()
					.flush()
					.executed();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		backendMock.verifyExpectationsMet();
	}

	private void initData() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			session.persist( new EntityWithPrimitiveId( 1 ) );
			session.persist( new EntityWithPrimitiveId( 2 ) );
			session.persist( new EntityWithPrimitiveId( 3 ) );
		} );
	}

	@Entity
	@Table(name = "book")
	@Indexed(index = EntityWithPrimitiveId.INDEX)
	public static class EntityWithPrimitiveId {

		public static final String INDEX = "EntityWithPrimitiveId";

		@Id
		private int id;

		public EntityWithPrimitiveId() {
		}

		public EntityWithPrimitiveId(int id) {
			this.id = id;
		}
	}
}

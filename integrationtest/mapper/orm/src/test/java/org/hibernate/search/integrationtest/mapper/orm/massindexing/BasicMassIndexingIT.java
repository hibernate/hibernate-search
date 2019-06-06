/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Fail.fail;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmAutomaticIndexingStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Very basic test to probe an use of {@link MassIndexer} api.
 */
public class BasicMassIndexingIT {

	public static final String TITLE_1 = "Oliver Twist";
	public static final String AUTHOR_1 = "Charles Dickens";
	public static final String TITLE_2 = "Ulysses";
	public static final String AUTHOR_2 = "James Joyce";
	public static final String TITLE_3 = "Frankenstein";
	public static final String AUTHOR_3 = "Mary Shelley";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = new OrmSetupHelper();

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectAnySchema( Book.INDEX );

		sessionFactory = ormSetupHelper.withBackendMock( backendMock )
				.withPropertyRadical( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_STRATEGY, HibernateOrmAutomaticIndexingStrategyName.NONE )
				.setup( Book.class );

		backendMock.verifyExpectationsMet();

		initData();
	}

	@Test
	public void defaultMassIndexerStartAndWait() throws Exception {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.getSearchSession( session );
			MassIndexer indexer = searchSession.createIndexer();

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorksAnyOrder(
					Book.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "1", b -> b
							.field( "title", TITLE_1 )
							.field( "author", AUTHOR_1 )
					)
					.add( "2", b -> b
							.field( "title", TITLE_2 )
							.field( "author", AUTHOR_2 )
					)
					.add( "3", b -> b
							.field( "title", TITLE_3 )
							.field( "author", AUTHOR_3 )
					)
					.preparedThenExecuted();

			// purgeAtStart, optimizeAfterPurge and purgeAtStart flags are active by default,
			// so we expect 1 purge, 2 optimize and 1 flush calls in this order:
			backendMock.expectWorks( Book.INDEX )
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

	@Test
	public void reuseSearchSessionAfterOrmSessionIsClosed_createMassIndexer() {
		Session session = sessionFactory.openSession();
		SearchSession searchSession = Search.getSearchSession( session );
		// a SearchSession instance is created lazily,
		// so we need to use it to have an instance of it
		searchSession.createIndexer();
		session.close();

		SubTest.expectException( () -> {
			searchSession.createIndexer();
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessage( "HSEARCH800017: Underlying Hibernate ORM Session seems to be closed." );
	}

	@Test
	public void lazyCrateSearchSessionAfterOrmSessionIsClosed_createMassIndexer() {
		Session session = sessionFactory.openSession();
		// Search session is not created, since we don't use it
		SearchSession searchSession = Search.getSearchSession( session );
		session.close();

		SubTest.expectException( () -> {
			searchSession.createIndexer();
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessage( "HSEARCH800016: Error trying to access Hibernate ORM session." );
	}

	private void initData() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			session.persist( new Book( 1, TITLE_1, AUTHOR_1 ) );
			session.persist( new Book( 2, TITLE_2, AUTHOR_2 ) );
			session.persist( new Book( 3, TITLE_3, AUTHOR_3 ) );
		} );
	}

	@Entity
	@Table(name = "book")
	@Indexed(index = Book.INDEX)
	public static class Book {

		public static final String INDEX = "Book";

		@Id
		private Integer id;

		@GenericField
		private String title;

		@GenericField
		private String author;

		public Book() {
		}

		public Book(Integer id, String title, String author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}

		public Integer getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public String getAuthor() {
			return author;
		}
	}
}

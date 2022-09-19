/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Fail.fail;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test mass indexing of an entity type whose document ID is not the entity ID.
 */
public class MassIndexingNonEntityIdDocumentIdIT {

	private static final String TITLE_1 = "Oliver Twist";
	private static final String AUTHOR_1 = "Charles Dickens";
	private static final String TITLE_2 = "Ulysses";
	private static final String AUTHOR_2 = "James Joyce";
	private static final String TITLE_3 = "Frankenstein";
	private static final String AUTHOR_3 = "Mary Shelley";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectAnySchema( Book.INDEX );

		sessionFactory = ormSetupHelper.start()
				.withPropertyRadical( HibernateOrmMapperSettings.Radicals.INDEXING_LISTENERS_ENABLED, false )
				.setup( Book.class );

		backendMock.verifyExpectationsMet();

		initData();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3203")
	public void defaultMassIndexerStartAndWait() {
		with( sessionFactory ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer();

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					Book.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "41", b -> b
							.field( "title", TITLE_1 )
							.field( "author", AUTHOR_1 )
					)
					.add( "42", b -> b
							.field( "title", TITLE_2 )
							.field( "author", AUTHOR_2 )
					)
					.add( "43", b -> b
							.field( "title", TITLE_3 )
							.field( "author", AUTHOR_3 )
					);

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( Book.INDEX, session.getTenantIdentifier() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

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
		with( sessionFactory ).runInTransaction( session -> {
			session.persist( new Book( 1, 41, TITLE_1, AUTHOR_1 ) );
			session.persist( new Book( 2, 42, TITLE_2, AUTHOR_2 ) );
			session.persist( new Book( 3, 43, TITLE_3, AUTHOR_3 ) );
		} );
	}

	@Entity
	@Table(name = "book")
	@Indexed(index = Book.INDEX)
	public static class Book {

		public static final String INDEX = "Book";

		@Id
		private Integer id;

		@DocumentId
		private Integer documentId;

		@GenericField
		private String title;

		@GenericField
		private String author;

		protected Book() {
		}

		Book(int id, int documentId, String title, String author) {
			this.id = id;
			this.documentId = documentId;
			this.title = title;
			this.author = author;
		}
	}
}

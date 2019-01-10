/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.hibernate.search.util.impl.integrationtest.orm.OrmUtils.withinSession;
import static org.hibernate.search.util.impl.integrationtest.orm.OrmUtils.withinTransaction;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.backend.elasticsearch.cfg.SearchBackendElasticsearchSettings;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchBackendFactory;
import org.hibernate.search.integrationtest.showcase.library.analysis.LibraryAnalysisConfigurer;
import org.hibernate.search.integrationtest.showcase.library.dao.DaoFactory;
import org.hibernate.search.integrationtest.showcase.library.dao.DocumentDao;
import org.hibernate.search.integrationtest.showcase.library.dao.syntax.lambda.LambdaSyntaxDaoFactory;
import org.hibernate.search.integrationtest.showcase.library.model.Account;
import org.hibernate.search.integrationtest.showcase.library.model.Book;
import org.hibernate.search.integrationtest.showcase.library.model.BookCopy;
import org.hibernate.search.integrationtest.showcase.library.model.Borrowal;
import org.hibernate.search.integrationtest.showcase.library.model.Document;
import org.hibernate.search.integrationtest.showcase.library.model.DocumentCopy;
import org.hibernate.search.integrationtest.showcase.library.model.ISBN;
import org.hibernate.search.integrationtest.showcase.library.model.Library;
import org.hibernate.search.integrationtest.showcase.library.model.Person;
import org.hibernate.search.integrationtest.showcase.library.model.Video;
import org.hibernate.search.integrationtest.showcase.library.model.VideoCopy;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.IndexingStrategyConfiguration;
import org.hibernate.search.mapper.orm.cfg.SearchOrmSettings;
import org.hibernate.search.mapper.orm.hibernate.FullTextQuery;
import org.hibernate.search.mapper.orm.hibernate.FullTextSession;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.Action;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ManualIndexingIT {

	private static final String PREFIX = SearchOrmSettings.PREFIX;
	private static final int NUMBER_OF_BOOKS = 200;
	private static final int MASS_INDEXING_MONITOR_LOG_PERIOD = 50; // This is the default in the implementation, do not change this value
	static {
		if ( NUMBER_OF_BOOKS < 2 * MASS_INDEXING_MONITOR_LOG_PERIOD ) {
			throw new IllegalStateException(
					"There's a bug in tests: NUMBER_OF_BOOKS should be strictly higher than two times "
							+ MASS_INDEXING_MONITOR_LOG_PERIOD
			);
		}
	}

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	private final DaoFactory daoFactory;

	private SessionFactory sessionFactory;

	public ManualIndexingIT() {
		this.daoFactory = new LambdaSyntaxDaoFactory();
	}

	@Before
	public void setup() {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
				.applySetting( PREFIX + "backends.elasticsearchBackend_1.type", ElasticsearchBackendFactory.class.getName() )
				.applySetting( PREFIX + "default_backend", "elasticsearchBackend_1" )
				.applySetting( PREFIX + "backends.elasticsearchBackend_1.log.json_pretty_printing", true )
				.applySetting(
						PREFIX + "backends.elasticsearchBackend_1." + SearchBackendElasticsearchSettings.ANALYSIS_CONFIGURER,
						new LibraryAnalysisConfigurer()
				)
				.applySetting( org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
				// disable auto indexing:
				.applySetting( SearchOrmSettings.INDEXING_STRATEGY, IndexingStrategyConfiguration.MANUAL );

		ServiceRegistry serviceRegistry = registryBuilder.build();

		MetadataSources ms = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Document.class )
				.addAnnotatedClass( Book.class )
				.addAnnotatedClass( Video.class )
				.addAnnotatedClass( Library.class )
				.addAnnotatedClass( DocumentCopy.class )
				.addAnnotatedClass( BookCopy.class )
				.addAnnotatedClass( VideoCopy.class )
				.addAnnotatedClass( Person.class )
				.addAnnotatedClass( Account.class )
				.addAnnotatedClass( Borrowal.class );

		Metadata metadata = ms.buildMetadata();

		final SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();
		this.sessionFactory = sfb.build();

		withinTransaction( sessionFactory, this::initData );
	}

	@After
	public void cleanup() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	@Test
	public void testMassIndexing() {
		withinSession( sessionFactory, this::checkNothingIsIndexed );
		withinTransaction( sessionFactory, session -> {
			FullTextSession ftSession = Search.getFullTextSession( session );
			MassIndexer indexer = ftSession.createIndexer();
			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}
		} );
		withinSession( sessionFactory, this::checkEverythingIsIndexed );
	}

	@Test
	public void testMassIndexingMonitor() {
		withinSession( sessionFactory, this::checkNothingIsIndexed );

		withinTransaction( sessionFactory, session -> {
			FullTextSession ftSession = Search.getFullTextSession( session );
			MassIndexer indexer = ftSession.createIndexer();
			try {
				/*
				 * The default period for logging in the default mass indexing monitor is 50.
				 * We set the batch size to 49.
				 * 50 = 5*5*2
				 * 49 = 7*7
				 * Thus a multiple of 49 cannot be a multiple of 50,
				 * and if we set the batch size to 49, the bug described in HSEARCH-3462
				 * will prevent any log from ever happening, except at the very end
				 *
				 * Regardless of this bug, here we also check that the mass indexing monitor works correctly:
				 * the number of log events should be equal to NUMBER_OF_BOOKS / 50.
				 */
				int batchSize = 49;
				indexer.batchSizeToLoadObjects( batchSize );
				int expectedNumberOfLogs = NUMBER_OF_BOOKS / MASS_INDEXING_MONITOR_LOG_PERIOD;
				logged.expectMessage( "documents indexed in" ).times( expectedNumberOfLogs );
				logged.expectMessage( "Indexing speed: " ).times( expectedNumberOfLogs );

				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}
		} );
		withinSession( sessionFactory, this::checkEverythingIsIndexed );
	}

	private void initData(Session session) {
		DocumentDao documentDao = daoFactory.createDocumentDao( session );
		for ( int i = 0; i < NUMBER_OF_BOOKS; i++ ) {
			addBook( documentDao, i );
		}
	}

	private void addBook(DocumentDao documentDao, int index) {
		String isbn = String.format( Locale.ROOT, "973-0-00-%06d-3", index );

		documentDao.createBook(
				index, new ISBN( isbn ), "Divine Comedy chapter n. " + ( index + 1 ),
				"The Divine Comedy is composed of 14,233 lines that are divided into three cantiche (singular cantica) – Inferno (Hell), Purgatorio (Purgatory), and Paradiso (Paradise)",
				"literature,poem,afterlife"
		);
	}

	private void checkNothingIsIndexed(Session session) {
		FullTextSession ftSession = Search.getFullTextSession( session );
		FullTextQuery<Book> query = ftSession.search( Book.class ).query().asEntity()
				.predicate( context -> context.matchAll().toPredicate() ).build();
		List<Book> books = query.getResultList();

		assertThat( books ).hasSize( 0 );
	}

	private void checkEverythingIsIndexed(Session session) {
		DocumentDao dao = daoFactory.createDocumentDao( session );

		assertThat( dao.count() ).isEqualTo( NUMBER_OF_BOOKS );

		Optional<Book> book = dao.getByIsbn( "973-0-00-000007-3" );
		assertTrue( book.isPresent() );
		assertThat( book.get() ).isEqualTo( session.get( Book.class, 7 ) );
	}
}

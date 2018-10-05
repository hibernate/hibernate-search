/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.orm.OrmUtils.withinSession;
import static org.hibernate.search.util.impl.integrationtest.orm.OrmUtils.withinTransaction;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import org.hibernate.search.mapper.orm.cfg.SearchOrmSettings;
import org.hibernate.search.integrationtest.showcase.library.bridge.AccountBorrowalSummaryBridge;
import org.hibernate.search.integrationtest.showcase.library.dao.DaoFactory;
import org.hibernate.search.integrationtest.showcase.library.dao.DocumentDao;
import org.hibernate.search.integrationtest.showcase.library.dao.LibraryDao;
import org.hibernate.search.integrationtest.showcase.library.dao.PersonDao;
import org.hibernate.search.integrationtest.showcase.library.dao.syntax.fluidandlambda.FluidAndLambdaSyntaxDaoFactory;
import org.hibernate.search.integrationtest.showcase.library.dao.syntax.fluidandobject.FluidAndObjectSyntaxDaoFactory;
import org.hibernate.search.integrationtest.showcase.library.dao.syntax.lambda.LambdaSyntaxDaoFactory;
import org.hibernate.search.integrationtest.showcase.library.dao.syntax.object.ObjectSyntaxDaoFactory;
import org.hibernate.search.integrationtest.showcase.library.model.Account;
import org.hibernate.search.integrationtest.showcase.library.model.Book;
import org.hibernate.search.integrationtest.showcase.library.model.BookCopy;
import org.hibernate.search.integrationtest.showcase.library.model.BookMedium;
import org.hibernate.search.integrationtest.showcase.library.model.Borrowal;
import org.hibernate.search.integrationtest.showcase.library.model.BorrowalType;
import org.hibernate.search.integrationtest.showcase.library.model.Document;
import org.hibernate.search.integrationtest.showcase.library.model.DocumentCopy;
import org.hibernate.search.integrationtest.showcase.library.model.ISBN;
import org.hibernate.search.integrationtest.showcase.library.model.Library;
import org.hibernate.search.integrationtest.showcase.library.model.LibraryService;
import org.hibernate.search.integrationtest.showcase.library.model.Person;
import org.hibernate.search.integrationtest.showcase.library.model.ProgrammaticMappingConfigurer;
import org.hibernate.search.integrationtest.showcase.library.model.Video;
import org.hibernate.search.integrationtest.showcase.library.model.VideoCopy;
import org.hibernate.search.integrationtest.showcase.library.model.VideoMedium;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.ImmutableGeoPoint;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.Action;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class OrmElasticsearchLibraryShowcaseIT {

	private enum MappingMode {
		ANNOTATION_MAPPING,
		PROGRAMMATIC_MAPPING;
	}

	@Parameterized.Parameters(name = "{0} - {1}")
	public static Object[][] parameters() {
		MappingMode[] mappingModes = new MappingMode[] {
				MappingMode.ANNOTATION_MAPPING,
				MappingMode.PROGRAMMATIC_MAPPING
		};
		DaoFactory[] daoFactories = new DaoFactory[] {
				new FluidAndLambdaSyntaxDaoFactory(),
				new FluidAndObjectSyntaxDaoFactory(),
				new LambdaSyntaxDaoFactory(),
				new ObjectSyntaxDaoFactory()
		};
		// Compute the cross product
		Object[][] parameters = new Object[mappingModes.length * daoFactories.length][2];
		int i = 0;
		for ( MappingMode mappingMode : mappingModes ) {
			for ( DaoFactory daoFactory : daoFactories ) {
				parameters[i][0] = mappingMode;
				parameters[i][1] = daoFactory;
				++i;
			}
		}
		return parameters;
	}

	private static final String PREFIX = SearchOrmSettings.PREFIX;

	// Document IDs
	private static final int CALLIGRAPHY_ID = 1;
	private static final int JAVA_DANCING_ID = 2;
	private static final int INDONESIAN_ECONOMY_ID = 3;
	private static final int JAVA_FOR_DUMMIES_ID = 4;
	private static final int ART_OF_COMPUTER_PROG_ID = 5;
	private static final int THESAURUS_OF_LANGUAGES_ID = 6;
	private static final int LIVING_ON_ISLAND_ID = 7;

	// Library IDs
	private static final int CITY_CENTER_ID = 1;
	private static final int SUBURBAN_1_ID = 2;
	private static final int SUBURBAN_2_ID = 3;
	private static final int UNIVERSITY_ID = 4;

	// Person IDs
	private static final int JANE_SMITH_ID = 1;
	private static final int JANE_FONDA_ID = 2;
	private static final int JANE_PORTER_ID = 3;
	private static final int JOHN_LENNON_ID = 4;
	private static final int ELTON_JOHN_ID = 5;
	private static final int PATTY_SMITH_ID = 6;
	private static final int JOHN_SMITH_ID = 7;
	private static final int JOHN_PAUL_SMITH_ID = 8;
	private static final int JOHN_PAUL_ID = 9;
	private static final int PAUL_JOHN_ID = 10;

	private final MappingMode mappingMode;
	private final DaoFactory daoFactory;

	private SessionFactory sessionFactory;

	public OrmElasticsearchLibraryShowcaseIT(MappingMode mappingMode, DaoFactory daoFactory) {
		this.mappingMode = mappingMode;
		this.daoFactory = daoFactory;
	}

	@Before
	public void setup() {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
				.applySetting( PREFIX + "backend.elasticsearchBackend_1.type", ElasticsearchBackendFactory.class.getName() )
				.applySetting( PREFIX + "index.default.backend", "elasticsearchBackend_1" )
				.applySetting( PREFIX + "backend.elasticsearchBackend_1.log.json_pretty_printing", true )
				.applySetting(
						PREFIX + "backend.elasticsearchBackend_1." + SearchBackendElasticsearchSettings.ANALYSIS_CONFIGURER,
						new LibraryAnalysisConfigurer()
				)
				.applySetting( org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP );

		if ( MappingMode.PROGRAMMATIC_MAPPING.equals( mappingMode ) ) {
			registryBuilder.applySetting( SearchOrmSettings.ENABLE_ANNOTATION_MAPPING, "false" );
			registryBuilder.applySetting( SearchOrmSettings.MAPPING_CONFIGURER,
					new ProgrammaticMappingConfigurer() );
		}

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
	}

	@After
	public void cleanup() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	@Test
	public void search_library() {
		withinTransaction( sessionFactory, this::initData );

		withinSession( sessionFactory, session -> {
			LibraryDao dao = daoFactory.createLibraryDao( session );

			List<Library> libraries = dao.search( "library", 0, 10 );
			assertThat( libraries ).containsExactly(
					session.get( Library.class, CITY_CENTER_ID ),
					session.get( Library.class, UNIVERSITY_ID ), // Bumped to this position because of its collection size
					session.get( Library.class, SUBURBAN_1_ID ),
					session.get( Library.class, SUBURBAN_2_ID )
			);
			libraries = dao.search( "library", 1, 2 );
			assertThat( libraries ).containsExactly(
					session.get( Library.class, UNIVERSITY_ID ),
					session.get( Library.class, SUBURBAN_1_ID )
			);
			libraries = dao.search( "sUburban", 0, 10 );
			assertThat( libraries ).containsExactly(
					session.get( Library.class, SUBURBAN_1_ID ),
					session.get( Library.class, SUBURBAN_2_ID )
			);
			// TODO introduce an AND operator in the match query to make this match SUBURBAN_1_ID only
			libraries = dao.search( "Suburban 1", 0, 10 );
			assertThat( libraries ).containsExactly(
					session.get( Library.class, SUBURBAN_1_ID ),
					session.get( Library.class, SUBURBAN_2_ID )
			);
			libraries = dao.search( "city center", 0, 10 );
			assertThat( libraries ).containsExactly(
					session.get( Library.class, CITY_CENTER_ID )
			);
		} );
	}

	@Test
	public void search_person() {
		withinTransaction( sessionFactory, this::initData );

		withinSession( sessionFactory, session -> {
			PersonDao dao = daoFactory.createPersonDao( sessionFactory.createEntityManager() );

			List<Person> results = dao.search(
					"smith", 0, 10
			);
			assertThat( results ).containsExactly(
					session.get( Person.class, JANE_SMITH_ID ),
					session.get( Person.class, JOHN_SMITH_ID ),
					session.get( Person.class, JOHN_PAUL_SMITH_ID ),
					session.get( Person.class, PATTY_SMITH_ID )
			);

			results = dao.search(
					"john", 0, 10
			);
			assertThat( results ).containsExactly(
					session.get( Person.class, ELTON_JOHN_ID ),
					session.get( Person.class, PAUL_JOHN_ID ),
					session.get( Person.class, JOHN_LENNON_ID ),
					session.get( Person.class, JOHN_PAUL_ID ),
					session.get( Person.class, JOHN_SMITH_ID ),
					session.get( Person.class, JOHN_PAUL_SMITH_ID )
			);

			// TODO introduce an AND operator in the match query to make this match JOHN_SMITH_ID and JOHN_PAUL_SMITH_ID only
			results = dao.search(
					"john smith", 0, 10
			);
			assertThat( results ).containsExactly(
					session.get( Person.class, ELTON_JOHN_ID ),
					session.get( Person.class, PAUL_JOHN_ID ),
					session.get( Person.class, JOHN_LENNON_ID ),
					session.get( Person.class, JOHN_PAUL_ID ),
					session.get( Person.class, JANE_SMITH_ID ),
					session.get( Person.class, JOHN_SMITH_ID ),
					session.get( Person.class, JOHN_PAUL_SMITH_ID ),
					session.get( Person.class, PATTY_SMITH_ID )
			);
		} );
	}

	@Test
	public void search_single() {
		withinTransaction( sessionFactory, this::initData );

		withinSession( sessionFactory, session -> {
			DocumentDao dao = daoFactory.createDocumentDao( session );

			Optional<Book> book = dao.getByIsbn( "978-0-00-000001-1" );
			assertTrue( book.isPresent() );
			assertThat( book.get() ).isEqualTo( session.get( Book.class, CALLIGRAPHY_ID ) );

			book = dao.getByIsbn( "978-0-00-000005-5" );
			assertTrue( book.isPresent() );
			assertThat( book.get() ).isEqualTo( session.get( Book.class, ART_OF_COMPUTER_PROG_ID ) );

			book = dao.getByIsbn( "978-0-00-000005-1" );
			assertFalse( book.isPresent() );

			// Test the normalizer
			book = dao.getByIsbn( "9780000000055" );
			assertTrue( book.isPresent() );
			assertThat( book.get() ).isEqualTo( session.get( Book.class, ART_OF_COMPUTER_PROG_ID ) );
		} );
	}

	/**
	 * This demonstrates generics are resolved properly, since the field "medium" doesn't appear in {@link DocumentCopy}
	 * and could only exist in the index if the "copies" property in class {@link Book}
	 * was successfully resolved to {@code List<BookCopy>}.
	 */
	@Test
	public void searchByMedium() {
		withinTransaction( sessionFactory, this::initData );

		withinSession( sessionFactory, session -> {
			DocumentDao dao = daoFactory.createDocumentDao( session );

			List<Book> books = dao.searchByMedium(
					"java", BookMedium.DEMATERIALIZED, 0, 10
			);
			assertThat( books ).containsOnly(
					session.get( Book.class, JAVA_FOR_DUMMIES_ID )
			);

			books = dao.searchByMedium(
					"java", BookMedium.HARDCOPY, 0, 10
			);
			assertThat( books ).containsOnly(
					session.get( Book.class, JAVA_FOR_DUMMIES_ID ),
					session.get( Book.class, INDONESIAN_ECONOMY_ID )
			);
		} );
	}

	@Test
	public void searchAroundMe_spatial() {
		withinTransaction( sessionFactory, this::initData );

		withinSession( sessionFactory, session -> {
			DocumentDao dao = daoFactory.createDocumentDao( session );

			GeoPoint myLocation = new ImmutableGeoPoint( 42.0, 0.5 );

			List<Document<?>> documents = dao.searchAroundMe(
					null, null,
					myLocation, 20.0,
					null,
					0, 10
			);
			// Should only include content from university
			assertThat( documents ).containsExactly(
					session.get( Book.class, INDONESIAN_ECONOMY_ID ),
					session.get( Book.class, JAVA_FOR_DUMMIES_ID ),
					session.get( Book.class, ART_OF_COMPUTER_PROG_ID ),
					session.get( Book.class, THESAURUS_OF_LANGUAGES_ID )
			);

			documents = dao.searchAroundMe(
					null, null,
					myLocation, 40.0,
					null,
					0, 10
			);
			// Should only include content from suburb1 or university
			assertThat( documents ).containsExactly(
					session.get( Book.class, CALLIGRAPHY_ID ),
					session.get( Book.class, INDONESIAN_ECONOMY_ID ),
					session.get( Book.class, JAVA_FOR_DUMMIES_ID ),
					session.get( Book.class, ART_OF_COMPUTER_PROG_ID ),
					session.get( Book.class, THESAURUS_OF_LANGUAGES_ID )
			);

			documents = dao.searchAroundMe(
					"calligraphy", null,
					myLocation, 40.0,
					null,
					0, 10
			);
			// Should only include content from suburb1 or university with "calligraphy" in it
			assertThat( documents ).containsExactly(
					session.get( Book.class, CALLIGRAPHY_ID )
			);

			myLocation = new ImmutableGeoPoint( 42.0, 0.75 );
			documents = dao.searchAroundMe(
					null, null,
					myLocation, 40.0,
					null,
					0, 10
			);
			// Should only include content from university
			assertThat( documents ).containsExactly(
					session.get( Book.class, INDONESIAN_ECONOMY_ID ),
					session.get( Book.class, JAVA_FOR_DUMMIES_ID ),
					session.get( Book.class, ART_OF_COMPUTER_PROG_ID ),
					session.get( Book.class, THESAURUS_OF_LANGUAGES_ID )
			);
		} );
	}

	@Test
	public void searchAroundMe_nested() {
		withinTransaction( sessionFactory, this::initData );

		withinSession( sessionFactory, session -> {
			DocumentDao dao = daoFactory.createDocumentDao( session );

			List<Document<?>> documents = dao.searchAroundMe(
					"java", null,
					null, null,
					Collections.singletonList( LibraryService.DISABLED_ACCESS ),
					0, 10
			);
			assertThat( documents ).containsOnly(
					session.get( Book.class, JAVA_FOR_DUMMIES_ID ),
					session.get( Video.class, JAVA_DANCING_ID ),
					session.get( Book.class, INDONESIAN_ECONOMY_ID )
			);

			documents = dao.searchAroundMe(
					"java", null,
					null, null,
					Collections.singletonList( LibraryService.READING_ROOMS ),
					0, 10
			);
			assertThat( documents ).containsOnly(
					session.get( Book.class, JAVA_FOR_DUMMIES_ID ),
					session.get( Video.class, JAVA_DANCING_ID ),
					session.get( Book.class, INDONESIAN_ECONOMY_ID )
			);

			documents = dao.searchAroundMe(
					"java", null,
					null, null,
					Arrays.asList( LibraryService.DISABLED_ACCESS, LibraryService.READING_ROOMS ),
					0, 10
			);
			/*
			 * In particular, should not match the document "indonesianEconomy",
			 * which is present in a library with disabled access and in a library with reading rooms,
			 * but not in a library with both.
			 */
			assertThat( documents ).containsOnly(
					session.get( Book.class, JAVA_FOR_DUMMIES_ID ),
					session.get( Video.class, JAVA_DANCING_ID )
			);
		} );
	}

	@Test
	public void searchAroundMe_searchBridge() {
		withinTransaction( sessionFactory, this::initData );

		withinSession( sessionFactory, session -> {
			DocumentDao dao = daoFactory.createDocumentDao( session );

			List<Document<?>> documents = dao.searchAroundMe(
					null, "java",
					null, null,
					null,
					0, 10
			);
			assertThat( documents ).containsOnly(
					session.get( Video.class, JAVA_DANCING_ID ),
					session.get( Book.class, JAVA_FOR_DUMMIES_ID ),
					session.get( Book.class, INDONESIAN_ECONOMY_ID ),
					session.get( Video.class, LIVING_ON_ISLAND_ID )
			);

			documents = dao.searchAroundMe(
					null, "programming",
					null, null,
					null,
					0, 10
			);
			assertThat( documents ).containsOnly(
					session.get( Book.class, JAVA_FOR_DUMMIES_ID ),
					session.get( Book.class, ART_OF_COMPUTER_PROG_ID )
			);

			documents = dao.searchAroundMe(
					null, "java,programming",
					null, null,
					null,
					0, 10
			);
			assertThat( documents ).containsOnly(
					session.get( Book.class, JAVA_FOR_DUMMIES_ID )
			);
		} );
	}

	/**
	 * This demonstrates how a non-trivial bridge ({@link AccountBorrowalSummaryBridge})
	 * can be used to index data derived from the main model,
	 * and how this indexed data can then be queried.
	 */
	@Test
	public void listTopBorrowers() {
		withinTransaction( sessionFactory, this::initData );

		withinSession( sessionFactory, session -> {
			PersonDao dao = daoFactory.createPersonDao( session );

			List<Person> results = dao.listTopBorrowers( 0, 3 );
			assertThat( results ).containsExactly(
					session.get( Person.class, JANE_SMITH_ID ),
					session.get( Person.class, JANE_FONDA_ID ),
					session.get( Person.class, JANE_PORTER_ID )
			);

			results = dao.listTopShortTermBorrowers( 0, 3 );
			assertThat( results ).containsExactly(
					session.get( Person.class, JANE_FONDA_ID ),
					session.get( Person.class, JANE_SMITH_ID ),
					session.get( Person.class, PAUL_JOHN_ID )
			);

			results = dao.listTopLongTermBorrowers( 0, 3 );
			assertThat( results ).containsExactly(
					session.get( Person.class, JANE_PORTER_ID ),
					session.get( Person.class, JANE_SMITH_ID ),
					session.get( Person.class, JOHN_SMITH_ID )
			);
		} );
	}

	@Test
	public void aggregation() {
		// TODO aggregation
		assumeTrue( "Aggregation not implemented yet", false );
	}

	private void initData(Session session) {
		LibraryDao libraryDao = daoFactory.createLibraryDao( session );
		DocumentDao documentDao = daoFactory.createDocumentDao( session );
		PersonDao personDao = daoFactory.createPersonDao( session );

		Book calligraphy = documentDao.createBook(
				CALLIGRAPHY_ID,
				new ISBN( "978-0-00-000001-1" ),
				"Calligraphy for Dummies",
				"Learn to write artfully in ten lessons",
				"calligraphy,art"
		);

		Video javaDancing = documentDao.createVideo(
				JAVA_DANCING_ID,
				"Java le dire à tout le monde",
				"A brief history of Java dancing in Paris during the early 20th century",
				"java,dancing,history"
		);

		Book indonesianEconomy = documentDao.createBook(
				INDONESIAN_ECONOMY_ID,
				new ISBN( "978-0-00-000003-3" ),
				"Comparative Study of the Economy of Java and other Indonesian Islands",
				"Comparative study of the late 20th century economy of the main islands of Indonesia"
						+ " with accurate projections over the next ten centuries",
				"geography,economy,java,sumatra,borneo,sulawesi"
		);

		Book javaForDummies = documentDao.createBook(
				JAVA_FOR_DUMMIES_ID,
				new ISBN( "978-0-00-000004-4" ),
				// Use varying case on purpose
				"java for Dummies",
				"Learning the Java programming language in ten lessons",
				"programming,language,java"
		);

		Book artOfComputerProg = documentDao.createBook(
				ART_OF_COMPUTER_PROG_ID,
				new ISBN( "978-0-00-000005-5" ),
				"The Art of Computer Programming",
				"Quick review of basic computer programming principles in 965 chapters",
				"programming"
		);

		Book thesaurusOfLanguages = documentDao.createBook(
				THESAURUS_OF_LANGUAGES_ID,
				new ISBN( "978-0-00-000006-6" ),
				"Thesaurus of Indo-European Languages",
				"An entertaining list of about three thousand languages, most of which are long dead",
				"geography,language"
		);

		Video livingOnIsland = documentDao.createVideo(
				LIVING_ON_ISLAND_ID,
				"Living in an Island, Episode 3: Indonesia",
				"A journey across Indonesia's smallest islands depicting how island way of life differs from mainland living",
				"geography,java,sumatra,borneo,sulawesi"
		);

		// City center library
		Library cityCenterLibrary = libraryDao.create(
				CITY_CENTER_ID,
				"City Center Library",
				12400,
				42.0, 0.0,
				LibraryService.READING_ROOMS,
				LibraryService.HARDCOPY_LOAN
		);
		// Content: every document, but no dematerialized copy
		documentDao.createCopy( cityCenterLibrary, calligraphy, BookMedium.HARDCOPY );
		documentDao.createCopy( cityCenterLibrary, javaDancing, VideoMedium.DVD );
		documentDao.createCopy( cityCenterLibrary, indonesianEconomy, BookMedium.HARDCOPY );
		documentDao.createCopy( cityCenterLibrary, javaForDummies, BookMedium.HARDCOPY );
		documentDao.createCopy( cityCenterLibrary, artOfComputerProg, BookMedium.HARDCOPY );
		documentDao.createCopy( cityCenterLibrary, thesaurusOfLanguages, BookMedium.HARDCOPY );
		documentDao.createCopy( cityCenterLibrary, livingOnIsland, VideoMedium.BLURAY );

		// Suburban library 1
		Library suburbanLibrary1 = libraryDao.create(
				SUBURBAN_1_ID,
				// Use varying case on purpose
				"suburban Library 1",
				800,
				42.0, 0.25,
				LibraryService.DISABLED_ACCESS,
				LibraryService.HARDCOPY_LOAN
		);
		// Content: no video document
		documentDao.createCopy( suburbanLibrary1, calligraphy, BookMedium.HARDCOPY );
		documentDao.createCopy( suburbanLibrary1, indonesianEconomy, BookMedium.HARDCOPY );
		documentDao.createCopy( suburbanLibrary1, javaForDummies, BookMedium.HARDCOPY );
		documentDao.createCopy( suburbanLibrary1, artOfComputerProg, BookMedium.HARDCOPY );
		documentDao.createCopy( suburbanLibrary1, thesaurusOfLanguages, BookMedium.HARDCOPY );

		// Suburban library 2
		Library suburbanLibrary2 = libraryDao.create(
				SUBURBAN_2_ID,
				"Suburban Library 2",
				800, // Same as the other suburban library
				42.0, -0.25,
				LibraryService.DISABLED_ACCESS, LibraryService.READING_ROOMS,
				LibraryService.HARDCOPY_LOAN
		);
		// Content: no academic document, offers dematerialized copies
		documentDao.createCopy( suburbanLibrary2, calligraphy, BookMedium.HARDCOPY );
		documentDao.createCopy( suburbanLibrary2, calligraphy, BookMedium.DEMATERIALIZED );
		documentDao.createCopy( suburbanLibrary2, javaDancing, VideoMedium.DVD );
		documentDao.createCopy( suburbanLibrary2, javaDancing, VideoMedium.DEMATERIALIZED );
		documentDao.createCopy( suburbanLibrary2, javaForDummies, BookMedium.HARDCOPY );
		documentDao.createCopy( suburbanLibrary2, javaForDummies, BookMedium.DEMATERIALIZED );
		documentDao.createCopy( suburbanLibrary2, livingOnIsland, VideoMedium.BLURAY );
		documentDao.createCopy( suburbanLibrary2, livingOnIsland, VideoMedium.DEMATERIALIZED );

		// University library
		Library universityLibrary = libraryDao.create(
				UNIVERSITY_ID,
				"University Library",
				9000,
				42.0, 0.5,
				LibraryService.READING_ROOMS,
				LibraryService.HARDCOPY_LOAN, LibraryService.DEMATERIALIZED_LOAN
		);
		// Content: only academic and learning documents
		documentDao.createCopy( universityLibrary, indonesianEconomy, BookMedium.HARDCOPY );
		documentDao.createCopy( universityLibrary, javaForDummies, BookMedium.HARDCOPY );
		documentDao.createCopy( universityLibrary, artOfComputerProg, BookMedium.HARDCOPY );
		documentDao.createCopy( universityLibrary, thesaurusOfLanguages, BookMedium.HARDCOPY );

		Person janeSmith = personDao.create( JANE_SMITH_ID, "Jane", "Smith" );
		personDao.createAccount( janeSmith );
		createBorrowal( personDao, janeSmith, cityCenterLibrary, indonesianEconomy, BorrowalType.SHORT_TERM );
		createBorrowal( personDao, janeSmith, cityCenterLibrary, artOfComputerProg, BorrowalType.LONG_TERM );
		createBorrowal( personDao, janeSmith, cityCenterLibrary, javaForDummies, BorrowalType.SHORT_TERM );
		createBorrowal( personDao, janeSmith, cityCenterLibrary, calligraphy, BorrowalType.SHORT_TERM );
		createBorrowal( personDao, janeSmith, cityCenterLibrary, javaDancing, BorrowalType.LONG_TERM );

		Person janeFonda = personDao.create( JANE_FONDA_ID, "Jane", "Fonda" );
		personDao.createAccount( janeFonda );
		createBorrowal( personDao, janeFonda, universityLibrary, javaForDummies, BorrowalType.SHORT_TERM );
		createBorrowal( personDao, janeFonda, universityLibrary, artOfComputerProg, BorrowalType.SHORT_TERM );
		createBorrowal( personDao, janeFonda, universityLibrary, thesaurusOfLanguages, BorrowalType.SHORT_TERM );
		createBorrowal( personDao, janeFonda, universityLibrary, indonesianEconomy, BorrowalType.SHORT_TERM );

		// Use varying case on purpose
		Person janePorter = personDao.create( JANE_PORTER_ID, "Jane", "porter" );
		personDao.createAccount( janePorter );
		createBorrowal( personDao, janePorter, suburbanLibrary1, indonesianEconomy, BorrowalType.LONG_TERM );
		createBorrowal( personDao, janePorter, suburbanLibrary2, livingOnIsland, 1, BorrowalType.LONG_TERM );
		createBorrowal( personDao, janePorter, universityLibrary, thesaurusOfLanguages, BorrowalType.LONG_TERM );

		// Use varying case on purpose
		Person johnLennon = personDao.create( JOHN_LENNON_ID, "john", "Lennon" );
		personDao.createAccount( johnLennon );
		createBorrowal( personDao, johnLennon, suburbanLibrary2, javaDancing, 1, BorrowalType.SHORT_TERM );

		// Use varying case on purpose
		Person eltonJohn = personDao.create( ELTON_JOHN_ID, "elton", "john" );
		personDao.createAccount( eltonJohn );
		createBorrowal( personDao, eltonJohn, suburbanLibrary2, javaDancing, 1, BorrowalType.SHORT_TERM );

		Person pattySmith = personDao.create( PATTY_SMITH_ID, "Patty", "Smith" );
		personDao.createAccount( pattySmith );
		createBorrowal( personDao, pattySmith, suburbanLibrary2, javaDancing, 1, BorrowalType.SHORT_TERM );

		Person johnSmith = personDao.create( JOHN_SMITH_ID, "John", "Smith" );
		personDao.createAccount( johnSmith );
		createBorrowal( personDao, johnSmith, suburbanLibrary1, indonesianEconomy, BorrowalType.SHORT_TERM );
		createBorrowal( personDao, johnSmith, suburbanLibrary1, artOfComputerProg, BorrowalType.LONG_TERM );

		Person johnPaulSmith = personDao.create( JOHN_PAUL_SMITH_ID, "John Paul", "Smith" );
		// No account for this one

		Person johnPaul = personDao.create( JOHN_PAUL_ID, "John", "Paul" );
		personDao.createAccount( johnPaul );
		// This one has an account, but no borrowal

		Person paulJohn = personDao.create( PAUL_JOHN_ID, "Paul", "John" );
		personDao.createAccount( paulJohn );
		createBorrowal( personDao, paulJohn, cityCenterLibrary, javaForDummies, BorrowalType.SHORT_TERM );
		createBorrowal( personDao, paulJohn, cityCenterLibrary, artOfComputerProg, BorrowalType.SHORT_TERM );
	}

	// Helper methods

	private <D extends Document<C>, C extends DocumentCopy<D>> Borrowal createBorrowal(
			PersonDao personDao, Person person, Library library, D document, BorrowalType borrowalType) {
		return createBorrowal( personDao, person, library, document, 0, borrowalType );
	}

	private <D extends Document<C>, C extends DocumentCopy<D>> Borrowal createBorrowal(
			PersonDao personDao, Person person, Library library, D document, int copyIndex, BorrowalType borrowalType) {
		return personDao.createBorrowal(
				person.getAccount(),
				getCopy( library, document, copyIndex ),
				borrowalType
		);
	}

	private <D extends Document<C>, C extends DocumentCopy<D>> C getCopy(Library library, D document, int copyIndex) {
		return document.getCopies().stream()
				.filter( c -> c.getLibrary().equals( library ) )
				.skip( copyIndex )
				.findFirst()
				.orElseThrow( () -> new IllegalStateException(
						"The test setup is incorrect; could not find copy #" + copyIndex
								+ " of document " + document
								+ " for library " + library
				) );
	}
}

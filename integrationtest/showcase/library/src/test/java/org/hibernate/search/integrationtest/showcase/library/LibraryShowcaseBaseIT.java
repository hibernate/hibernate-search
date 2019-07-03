/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.ART_OF_COMPUTER_PROG_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.CALLIGRAPHY_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.CITY_CENTER_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.ELTON_JOHN_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.INDONESIAN_ECONOMY_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.JANE_FONDA_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.JANE_PORTER_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.JANE_SMITH_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.JAVA_DANCING_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.JAVA_FOR_DUMMIES_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.JOHN_LENNON_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.JOHN_PAUL_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.JOHN_PAUL_SMITH_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.JOHN_SMITH_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.LIVING_ON_ISLAND_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.PATTY_SMITH_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.PAUL_JOHN_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.SUBURBAN_1_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.SUBURBAN_2_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.THESAURUS_OF_LANGUAGES_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.UNIVERSITY_ID;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.showcase.library.bridge.AccountBorrowalSummaryBridge;
import org.hibernate.search.integrationtest.showcase.library.model.Book;
import org.hibernate.search.integrationtest.showcase.library.model.BookMedium;
import org.hibernate.search.integrationtest.showcase.library.model.Document;
import org.hibernate.search.integrationtest.showcase.library.model.DocumentCopy;
import org.hibernate.search.integrationtest.showcase.library.model.Library;
import org.hibernate.search.integrationtest.showcase.library.model.LibraryServiceOption;
import org.hibernate.search.integrationtest.showcase.library.model.Person;
import org.hibernate.search.integrationtest.showcase.library.service.BorrowalService;
import org.hibernate.search.integrationtest.showcase.library.service.DocumentService;
import org.hibernate.search.integrationtest.showcase.library.service.LibraryService;
import org.hibernate.search.integrationtest.showcase.library.service.TestDataService;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = { "automatic_indexing.strategy=session" })
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class LibraryShowcaseBaseIT {

	@Autowired
	private DocumentService documentService;

	@Autowired
	private LibraryService libraryService;

	@Autowired
	private BorrowalService borrowalService;

	@Autowired
	private TestDataService testDataService;

	@Before
	public void before() {
		testDataService.initDefaultDataSet();
	}

	@Test
	public void search_library() {
		List<Library> libraries = libraryService.search( "library", 0, 10 );
		assertThat( libraries ).extracting( Library::getId ).containsExactly(
				CITY_CENTER_ID,
				UNIVERSITY_ID, // Bumped to this position because of its collection size
				SUBURBAN_1_ID,
				SUBURBAN_2_ID );
		libraries = libraryService.search( "library", 1, 2 );
		assertThat( libraries ).extracting( Library::getId ).containsExactly(
				UNIVERSITY_ID,
				SUBURBAN_1_ID
		);
		libraries = libraryService.search( "sUburban", 0, 10 );
		assertThat( libraries ).extracting( Library::getId ).containsExactly(
				SUBURBAN_1_ID,
				SUBURBAN_2_ID
		);
		// TODO HSEARCH-917 introduce an AND operator in the match query to make this match SUBURBAN_1_ID only
		libraries = libraryService.search( "Suburban 1", 0, 10 );
		assertThat( libraries ).extracting( Library::getId ).containsExactly(
				SUBURBAN_1_ID,
				SUBURBAN_2_ID
		);
		libraries = libraryService.search( "city center", 0, 10 );
		assertThat( libraries ).extracting( Library::getId ).containsExactly(
				CITY_CENTER_ID
		);
	}

	@Test
	public void search_person() {
		List<Person> results = borrowalService.searchPerson(
				"smith", 0, 10
		);
		assertThat( results ).extracting( Person::getId ).containsExactly(
				JANE_SMITH_ID,
				JOHN_SMITH_ID,
				JOHN_PAUL_SMITH_ID,
				PATTY_SMITH_ID
		);

		results = borrowalService.searchPerson(
				"john", 0, 10
		);
		assertThat( results ).extracting( Person::getId ).containsExactly(
				ELTON_JOHN_ID,
				PAUL_JOHN_ID,
				JOHN_LENNON_ID,
				JOHN_PAUL_ID,
				JOHN_SMITH_ID,
				JOHN_PAUL_SMITH_ID
		);

		// TODO HSEARCH-917 introduce an AND operator in the match query to make this match JOHN_SMITH_ID and JOHN_PAUL_SMITH_ID only
		results = borrowalService.searchPerson(
				"john smith", 0, 10
		);
		assertThat( results ).extracting( Person::getId ).containsExactly(
				ELTON_JOHN_ID,
				PAUL_JOHN_ID,
				JOHN_LENNON_ID,
				JOHN_PAUL_ID,
				JANE_SMITH_ID,
				JOHN_SMITH_ID,
				JOHN_PAUL_SMITH_ID,
				PATTY_SMITH_ID
		);
	}

	@Test
	public void search_single() {
		Optional<Book> book = documentService.getByIsbn( "978-0-00-000001-1" );
		assertTrue( book.isPresent() );
		assertThat( book.get().getId() ).isEqualTo( CALLIGRAPHY_ID );

		book = documentService.getByIsbn( "978-0-00-000005-5" );
		assertTrue( book.isPresent() );
		assertThat( book.get().getId() ).isEqualTo( ART_OF_COMPUTER_PROG_ID );

		book = documentService.getByIsbn( "978-0-00-000005-1" );
		assertFalse( book.isPresent() );

		// Test the normalizer
		book = documentService.getByIsbn( "9780000000055" );
		assertTrue( book.isPresent() );
		assertThat( book.get().getId() ).isEqualTo( ART_OF_COMPUTER_PROG_ID );
	}

	/**
	 * This demonstrates generics are resolved properly, since the field "medium" doesn't appear in {@link DocumentCopy}
	 * and could only exist in the index if the "copies" property in class {@link Book}
	 * was successfully resolved to {@code List<BookCopy>}.
	 */
	@Test
	public void searchByMedium() {
		List<Book> books = documentService.searchByMedium(
				"java", BookMedium.DEMATERIALIZED, 0, 10
		);
		assertThat( books ).extracting( Book::getId ).containsExactlyInAnyOrder(
				JAVA_FOR_DUMMIES_ID
		);

		books = documentService.searchByMedium(
				"java", BookMedium.HARDCOPY, 0, 10
		);
		assertThat( books ).extracting( Book::getId ).containsExactlyInAnyOrder(
				JAVA_FOR_DUMMIES_ID,
				INDONESIAN_ECONOMY_ID
		);
	}

	@Test
	public void searchAroundMe_spatial() {
		GeoPoint myLocation = GeoPoint.of( 42.0, 0.5 );

		List<Document<?>> documents = documentService.searchAroundMe(
				null, null,
				myLocation, 20.0,
				null,
				0, 10
		);
		// Should only include content from university
		// TODO HSEARCH-2254 check results are sorted by distance once we do sort them (see the query implementation)
		assertThat( documents ).extracting( Document::getId ).containsExactlyInAnyOrder(
				INDONESIAN_ECONOMY_ID,
				JAVA_FOR_DUMMIES_ID,
				ART_OF_COMPUTER_PROG_ID,
				THESAURUS_OF_LANGUAGES_ID
		);

		documents = documentService.searchAroundMe(
				null, null,
				myLocation, 40.0,
				null,
				0, 10
		);
		// Should only include content from suburb1 or university
		// TODO HSEARCH-2254 check results are sorted by distance once we do sort them (see the query implementation)
		assertThat( documents ).extracting( Document::getId ).containsExactlyInAnyOrder(
				CALLIGRAPHY_ID,
				INDONESIAN_ECONOMY_ID,
				JAVA_FOR_DUMMIES_ID,
				ART_OF_COMPUTER_PROG_ID,
				THESAURUS_OF_LANGUAGES_ID
		);

		documents = documentService.searchAroundMe(
				"calligraphy", null,
				myLocation, 40.0,
				null,
				0, 10
		);
		// Should only include content from suburb1 or university with "calligraphy" in it
		// TODO HSEARCH-2254 check results are sorted by distance once we do sort them (see the query implementation)
		assertThat( documents ).extracting( Document::getId ).containsExactlyInAnyOrder(
				CALLIGRAPHY_ID
		);

		myLocation = GeoPoint.of( 42.0, 0.75 );
		documents = documentService.searchAroundMe(
				null, null,
				myLocation, 40.0,
				null,
				0, 10
		);
		// Should only include content from university
		// TODO HSEARCH-2254 check results are sorted by distance once we do sort them (see the query implementation)
		assertThat( documents ).extracting( Document::getId ).containsExactlyInAnyOrder(
				INDONESIAN_ECONOMY_ID,
				JAVA_FOR_DUMMIES_ID,
				ART_OF_COMPUTER_PROG_ID,
				THESAURUS_OF_LANGUAGES_ID
		);
	}

	@Test
	public void searchAroundMe_nested() {
		List<Document<?>> documents = documentService.searchAroundMe(
				"java", null,
				null, null,
				Collections.singletonList( LibraryServiceOption.DISABLED_ACCESS ),
				0, 10
		);
		assertThat( documents ).extracting( Document::getId ).containsExactlyInAnyOrder(
				JAVA_FOR_DUMMIES_ID,
				JAVA_DANCING_ID,
				INDONESIAN_ECONOMY_ID
		);

		documents = documentService.searchAroundMe(
				"java", null,
				null, null,
				Collections.singletonList( LibraryServiceOption.READING_ROOMS ),
				0, 10
		);
		assertThat( documents ).extracting( Document::getId ).containsExactlyInAnyOrder(
				JAVA_FOR_DUMMIES_ID,
				JAVA_DANCING_ID,
				INDONESIAN_ECONOMY_ID
		);

		documents = documentService.searchAroundMe(
				"java", null,
				null, null,
				Arrays.asList( LibraryServiceOption.DISABLED_ACCESS, LibraryServiceOption.READING_ROOMS ),
				0, 10
		);
		/*
		 * In particular, should not match the document "indonesianEconomy",
		 * which is present in a library with disabled access and in a library with reading rooms,
		 * but not in a library with both.
		 */
		assertThat( documents ).extracting( Document::getId ).containsExactlyInAnyOrder(
				JAVA_FOR_DUMMIES_ID,
				JAVA_DANCING_ID
		);
	}

	@Test
	public void searchAroundMe_searchBridge() {
		List<Document<?>> documents = documentService.searchAroundMe(
				null, "java",
				null, null,
				null,
				0, 10
		);
		assertThat( documents ).extracting( Document::getId ).containsExactlyInAnyOrder(
				JAVA_DANCING_ID,
				JAVA_FOR_DUMMIES_ID,
				INDONESIAN_ECONOMY_ID,
				LIVING_ON_ISLAND_ID
		);

		documents = documentService.searchAroundMe(
				null, "programming",
				null, null,
				null,
				0, 10
		);
		assertThat( documents ).extracting( Document::getId ).containsExactlyInAnyOrder(
				JAVA_FOR_DUMMIES_ID,
				ART_OF_COMPUTER_PROG_ID
		);

		documents = documentService.searchAroundMe(
				null, "java,programming",
				null, null,
				null,
				0, 10
		);
		assertThat( documents ).extracting( Document::getId ).containsExactlyInAnyOrder(
				JAVA_FOR_DUMMIES_ID
		);
	}

	/**
	 * This demonstrates how a non-trivial bridge ({@link AccountBorrowalSummaryBridge})
	 * can be used to index data derived from the main model,
	 * and how this indexed data can then be queried.
	 */
	@Test
	public void listTopBorrowers() {
		List<Person> results = borrowalService.listTopBorrowers( 0, 3 );
		assertThat( results ).extracting( Person::getId ).containsExactly(
				JANE_SMITH_ID,
				JANE_FONDA_ID,
				JANE_PORTER_ID
		);

		results = borrowalService.listTopShortTermBorrowers( 0, 3 );
		assertThat( results ).extracting( Person::getId ).containsExactly(
				JANE_FONDA_ID,
				JANE_SMITH_ID,
				PAUL_JOHN_ID
		);

		results = borrowalService.listTopLongTermBorrowers( 0, 3 );
		assertThat( results ).extracting( Person::getId ).containsExactly(
				JANE_PORTER_ID,
				JANE_SMITH_ID,
				JOHN_SMITH_ID
		);
	}

	/**
	 * This demonstrates how to define a projection for the query and how to set order.
	 */
	@Test
	// FIXME: re-enable this test once we properly implement PassThroughValueBridge binding
	@Ignore
	public void projectionAndOrder() {
		List<String> results = documentService.getAuthorsOfBooksHavingTerms( "java", SortOrder.ASC );
		assertThat( results ).containsExactly( "Mark Red", "Michele Violet", "Stuart Green" );

		results = documentService.getAuthorsOfBooksHavingTerms( "Indonesia", SortOrder.DESC );
		assertThat( results ).containsExactly( "Mark Red", "Mark Red" );
	}
}

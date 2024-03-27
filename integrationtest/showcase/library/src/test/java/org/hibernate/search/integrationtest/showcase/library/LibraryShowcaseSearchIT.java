/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.showcase.library.bridge.AccountBorrowalSummaryBridge;
import org.hibernate.search.integrationtest.showcase.library.dto.LibraryFacetedSearchResult;
import org.hibernate.search.integrationtest.showcase.library.dto.LibrarySimpleProjection;
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
import org.hibernate.search.util.common.data.Range;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class LibraryShowcaseSearchIT extends AbstractLibraryShowcaseSearchIT {

	private static boolean needsInit;

	@Autowired
	private DocumentService documentService;

	@Autowired
	private LibraryService libraryService;

	@Autowired
	private BorrowalService borrowalService;

	@Autowired
	private TestDataService testDataService;

	@BeforeAll
	static void beforeClass() {
		needsInit = true;
	}

	@BeforeEach
	void before() {
		if ( needsInit ) {
			testDataService.initDefaultDataSet();
			needsInit = false;
		}
	}

	@Test
	void search_library() {
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
	void search_person() {
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
	void search_single() {
		Optional<Book> book = documentService.getByIsbn( "978-0-00-000001-1" );
		assertThat( book ).isPresent()
				.get()
				.extracting( Document::getId )
				.isEqualTo( CALLIGRAPHY_ID );

		book = documentService.getByIsbn( "978-0-00-000005-5" );
		assertThat( book ).isPresent()
				.get()
				.extracting( Document::getId )
				.isEqualTo( ART_OF_COMPUTER_PROG_ID );

		book = documentService.getByIsbn( "978-0-00-000005-1" );
		assertThat( book ).isNotPresent();

		// Test the normalizer
		book = documentService.getByIsbn( "9780000000055" );
		assertThat( book ).isPresent()
				.get()
				.extracting( Document::getId )
				.isEqualTo( ART_OF_COMPUTER_PROG_ID );
	}

	/**
	 * This demonstrates generics are resolved properly, since the field "medium" doesn't appear in {@link DocumentCopy}
	 * and could only exist in the index if the "copies" property in class {@link Book}
	 * was successfully resolved to {@code List<BookCopy>}.
	 */
	@Test
	void searchByMedium() {
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
	void searchAroundMe_spatial() {
		GeoPoint myLocation = GeoPoint.of( 42.0, 0.5 );

		List<Document<?>> documents = documentService.searchAroundMe(
				null, null,
				myLocation, 20.0,
				null,
				0, 10
		);
		// Should only include content from university (my location).
		assertThat( documents ).extracting( Document::getId ).containsExactlyInAnyOrder(
				INDONESIAN_ECONOMY_ID,
				JAVA_FOR_DUMMIES_ID,
				ART_OF_COMPUTER_PROG_ID,
				THESAURUS_OF_LANGUAGES_ID
		);
		// Should be sorted by increasing distance,
		// but here all hits are in a library right at my location, so the sort doesn't matter.

		documents = documentService.searchAroundMe(
				null, null,
				myLocation, 40.0,
				null,
				0, 10
		);
		// Should only include content from suburb1 or university.
		assertThat( documents ).extracting( Document::getId ).containsExactlyInAnyOrder(
				CALLIGRAPHY_ID,
				INDONESIAN_ECONOMY_ID,
				JAVA_FOR_DUMMIES_ID,
				ART_OF_COMPUTER_PROG_ID,
				THESAURUS_OF_LANGUAGES_ID
		);
		// Should be sorted by increasing distance.
		// All books are in the university library right at my location,
		// except the book about calligraphy, which is in "suburb1".
		// So it should appear last.
		assertThat( documents ).extracting( Document::getId )
				.element( 4 ).isEqualTo( CALLIGRAPHY_ID );

		documents = documentService.searchAroundMe(
				"calligraphy", null,
				myLocation, 40.0,
				null,
				0, 10
		);
		// Should only include content from suburb1 or university with "calligraphy" in it.
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
		// Should only include content from university.
		assertThat( documents ).extracting( Document::getId ).containsExactlyInAnyOrder(
				INDONESIAN_ECONOMY_ID,
				JAVA_FOR_DUMMIES_ID,
				ART_OF_COMPUTER_PROG_ID,
				THESAURUS_OF_LANGUAGES_ID
		);
	}

	@Test
	void searchAroundMe_nested() {
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
	void searchAroundMe_searchBridge() {
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
	void listTopBorrowers() {
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
	void projectionAndOrder() {
		List<String> results = documentService.getAuthorsOfBooksHavingTerms( "java", SortOrder.ASC );
		assertThat( results ).containsExactly( "Mark Red", "Michele Violet", "Stuart Green" );

		results = documentService.getAuthorsOfBooksHavingTerms( "Indonesia", SortOrder.DESC );
		assertThat( results ).containsExactly( "Mark Red", "Mark Red" );
	}

	@Test
	void searchAndProject() {
		List<LibrarySimpleProjection> libraries = libraryService.searchAndProject( "library", 0, 10 );
		assertThat( libraries ).extracting( l -> l.name ).containsExactly(
				libraryService.getById( CITY_CENTER_ID, Library::getName ),
				libraryService.getById( UNIVERSITY_ID, Library::getName ),
				libraryService.getById( SUBURBAN_1_ID, Library::getName ),
				libraryService.getById( SUBURBAN_2_ID, Library::getName ) );
		assertThat( libraries ).extracting( l -> l.services ).containsExactly(
				libraryService.getById( CITY_CENTER_ID, Library::getServices ),
				libraryService.getById( UNIVERSITY_ID, Library::getServices ),
				libraryService.getById( SUBURBAN_1_ID, Library::getServices ),
				libraryService.getById( SUBURBAN_2_ID, Library::getServices ) );

		libraries = libraryService.searchAndProject( "library", 1, 2 );
		assertThat( libraries ).extracting( l -> l.name ).containsExactly(
				libraryService.getById( UNIVERSITY_ID, Library::getName ),
				libraryService.getById( SUBURBAN_1_ID, Library::getName ) );
		assertThat( libraries ).extracting( l -> l.services ).containsExactly(
				libraryService.getById( UNIVERSITY_ID, Library::getServices ),
				libraryService.getById( SUBURBAN_1_ID, Library::getServices ) );

		libraries = libraryService.searchAndProject( "sUburban", 0, 10 );
		assertThat( libraries ).extracting( l -> l.name ).containsExactly(
				libraryService.getById( SUBURBAN_1_ID, Library::getName ),
				libraryService.getById( SUBURBAN_2_ID, Library::getName ) );
		assertThat( libraries ).extracting( l -> l.services ).containsExactly(
				libraryService.getById( SUBURBAN_1_ID, Library::getServices ),
				libraryService.getById( SUBURBAN_2_ID, Library::getServices ) );
	}

	// This checks that method-local classes get automatically indexed by Hibernate Search with Jandex, in particular.
	@Test
	void searchAndProjectToMethodLocalClass() {
		List<LibrarySimpleProjection> libraries = libraryService.searchAndProjectToMethodLocalClass( "library", 0, 10 );
		assertThat( libraries ).extracting( l -> l.name ).containsExactly(
				libraryService.getById( CITY_CENTER_ID, Library::getName ),
				libraryService.getById( UNIVERSITY_ID, Library::getName ),
				libraryService.getById( SUBURBAN_1_ID, Library::getName ),
				libraryService.getById( SUBURBAN_2_ID, Library::getName ) );
		assertThat( libraries ).extracting( l -> l.services ).containsExactly(
				libraryService.getById( CITY_CENTER_ID, Library::getServices ),
				libraryService.getById( UNIVERSITY_ID, Library::getServices ),
				libraryService.getById( SUBURBAN_1_ID, Library::getServices ),
				libraryService.getById( SUBURBAN_2_ID, Library::getServices ) );

		libraries = libraryService.searchAndProjectToMethodLocalClass( "library", 1, 2 );
		assertThat( libraries ).extracting( l -> l.name ).containsExactly(
				libraryService.getById( UNIVERSITY_ID, Library::getName ),
				libraryService.getById( SUBURBAN_1_ID, Library::getName ) );
		assertThat( libraries ).extracting( l -> l.services ).containsExactly(
				libraryService.getById( UNIVERSITY_ID, Library::getServices ),
				libraryService.getById( SUBURBAN_1_ID, Library::getServices ) );

		libraries = libraryService.searchAndProjectToMethodLocalClass( "sUburban", 0, 10 );
		assertThat( libraries ).extracting( l -> l.name ).containsExactly(
				libraryService.getById( SUBURBAN_1_ID, Library::getName ),
				libraryService.getById( SUBURBAN_2_ID, Library::getName ) );
		assertThat( libraries ).extracting( l -> l.services ).containsExactly(
				libraryService.getById( SUBURBAN_1_ID, Library::getServices ),
				libraryService.getById( SUBURBAN_2_ID, Library::getServices ) );
	}

	@Test
	void searchFaceted() {
		LibraryFacetedSearchResult result = libraryService.searchFaceted(
				null, null, null,
				0, 10
		);
		assertThat( result.getTotalHitCountByCollectionSize() )
				.containsExactly(
						entry( Range.canonical( 0, 1_000 ), 2L ),
						entry( Range.canonical( 1_000, 5_000 ), 0L ),
						entry( Range.canonical( 5_000, 10_000 ), 1L ),
						entry( Range.canonical( 10_000, null ), 1L )
				);
		assertThat( result.getTotalHitCountByService() )
				.containsExactly(
						entry( LibraryServiceOption.DEMATERIALIZED_LOAN, 1L ),
						entry( LibraryServiceOption.DISABLED_ACCESS, 2L ),
						entry( LibraryServiceOption.HARDCOPY_LOAN, 4L ),
						entry( LibraryServiceOption.READING_ROOMS, 3L )
				);

		// Drill-down by collection size
		result = libraryService.searchFaceted(
				null, 1000, null,
				0, 10
		);
		assertThat( result.getTotalHitCountByCollectionSize() )
				.containsExactly(
						entry( Range.canonical( 0, 1_000 ), 0L ),
						entry( Range.canonical( 1_000, 5_000 ), 0L ),
						entry( Range.canonical( 5_000, 10_000 ), 1L ),
						entry( Range.canonical( 10_000, null ), 1L )
				);
		assertThat( result.getTotalHitCountByService() )
				.containsExactly(
						entry( LibraryServiceOption.DEMATERIALIZED_LOAN, 1L ),
						entry( LibraryServiceOption.DISABLED_ACCESS, 0L ),
						entry( LibraryServiceOption.HARDCOPY_LOAN, 2L ),
						entry( LibraryServiceOption.READING_ROOMS, 2L )
				);

		// Drill-down by service
		result = libraryService.searchFaceted(
				null, null,
				Arrays.asList( LibraryServiceOption.HARDCOPY_LOAN, LibraryServiceOption.READING_ROOMS ),
				0, 10
		);
		assertThat( result.getTotalHitCountByCollectionSize() )
				.containsExactly(
						entry( Range.canonical( 0, 1_000 ), 1L ),
						entry( Range.canonical( 1_000, 5_000 ), 0L ),
						entry( Range.canonical( 5_000, 10_000 ), 1L ),
						entry( Range.canonical( 10_000, null ), 1L )
				);
		assertThat( result.getTotalHitCountByService() )
				.containsExactly(
						entry( LibraryServiceOption.DEMATERIALIZED_LOAN, 1L ),
						entry( LibraryServiceOption.DISABLED_ACCESS, 1L ),
						entry( LibraryServiceOption.HARDCOPY_LOAN, 3L ),
						entry( LibraryServiceOption.READING_ROOMS, 3L )
				);
	}
}

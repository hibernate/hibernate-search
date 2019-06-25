/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.hibernate.search.integrationtest.showcase.library.LibraryShowcaseBaseIT;
import org.hibernate.search.integrationtest.showcase.library.model.Book;
import org.hibernate.search.integrationtest.showcase.library.model.BookMedium;
import org.hibernate.search.integrationtest.showcase.library.model.BorrowalType;
import org.hibernate.search.integrationtest.showcase.library.model.Library;
import org.hibernate.search.integrationtest.showcase.library.model.LibraryServiceOption;
import org.hibernate.search.integrationtest.showcase.library.model.Person;
import org.hibernate.search.integrationtest.showcase.library.model.Video;
import org.hibernate.search.integrationtest.showcase.library.model.VideoMedium;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TestDataService {

	@Autowired
	private DocumentService documentService;

	@Autowired
	private LibraryService libraryService;

	@Autowired
	private BorrowalService borrowalService;

	public void initDefaultDataSet() {
		Book calligraphy = documentService.createBook(
				LibraryShowcaseBaseIT.CALLIGRAPHY_ID,
				"978-0-00-000001-1",
				"Calligraphy for Dummies",
				"Roger Blue",
				"Learn to write artfully in ten lessons",
				"calligraphy,art"
		);

		Video javaDancing = documentService.createVideo(
				LibraryShowcaseBaseIT.JAVA_DANCING_ID,
				"Java le dire à tout le monde",
				"Michele Violet",
				"A brief history of Java dancing in Paris during the early 20th century",
				"java,dancing,history"
		);

		Book indonesianEconomy = documentService.createBook(
				LibraryShowcaseBaseIT.INDONESIAN_ECONOMY_ID,
				"978-0-00-000003-3",
				"Comparative Study of the Economy of Java and other Indonesian Islands",
				"Mark Red",
				"Comparative study of the late 20th century economy of the main islands of Indonesia"
						+ " with accurate projections over the next ten centuries",
				"geography,economy,java,sumatra,borneo,sulawesi"
		);

		Book javaForDummies = documentService.createBook(
				LibraryShowcaseBaseIT.JAVA_FOR_DUMMIES_ID,
				"978-0-00-000004-4",
				// Use varying case on purpose
				"java for Dummies",
				"Stuart Green",
				"Learning the Java programming language in ten lessons",
				"programming,language,java"
		);

		Book artOfComputerProg = documentService.createBook(
				LibraryShowcaseBaseIT.ART_OF_COMPUTER_PROG_ID,
				"978-0-00-000005-5",
				"The Art of Computer Programming",
				"Stuart Green",
				"Quick review of basic computer programming principles in 965 chapters",
				"programming"
		);

		Book thesaurusOfLanguages = documentService.createBook(
				LibraryShowcaseBaseIT.THESAURUS_OF_LANGUAGES_ID,
				"978-0-00-000006-6" ,
				"Thesaurus of Indo-European Languages",
				"Dorothy White",
				"An entertaining list of about three thousand languages, most of which are long dead",
				"geography,language"
		);

		Video livingOnIsland = documentService.createVideo(
				LibraryShowcaseBaseIT.LIVING_ON_ISLAND_ID,
				"Living in an Island, Episode 3: Indonesia",
				"Mark Red",
				"A journey across Indonesia's smallest islands depicting how island way of life differs from mainland living",
				"geography,java,sumatra,borneo,sulawesi"
		);

		// City center library
		Library cityCenterLibrary = libraryService.create(
				LibraryShowcaseBaseIT.CITY_CENTER_ID,
				"City Center Library",
				12400,
				42.0, 0.0,
				LibraryServiceOption.READING_ROOMS,
				LibraryServiceOption.HARDCOPY_LOAN
		);
		// Content: every document, but no dematerialized copy
		libraryService.createCopyInLibrary( cityCenterLibrary, calligraphy, BookMedium.HARDCOPY );
		libraryService.createCopyInLibrary( cityCenterLibrary, javaDancing, VideoMedium.DVD );
		libraryService.createCopyInLibrary( cityCenterLibrary, indonesianEconomy, BookMedium.HARDCOPY );
		libraryService.createCopyInLibrary( cityCenterLibrary, javaForDummies, BookMedium.HARDCOPY );
		libraryService.createCopyInLibrary( cityCenterLibrary, artOfComputerProg, BookMedium.HARDCOPY );
		libraryService.createCopyInLibrary( cityCenterLibrary, thesaurusOfLanguages, BookMedium.HARDCOPY );
		libraryService.createCopyInLibrary( cityCenterLibrary, livingOnIsland, VideoMedium.BLURAY );

		// Suburban library 1
		Library suburbanLibrary1 = libraryService.create(
				LibraryShowcaseBaseIT.SUBURBAN_1_ID,
				// Use varying case on purpose
				"suburban Library 1",
				800,
				42.0, 0.25,
				LibraryServiceOption.DISABLED_ACCESS,
				LibraryServiceOption.HARDCOPY_LOAN
		);
		// Content: no video document
		libraryService.createCopyInLibrary( suburbanLibrary1, calligraphy, BookMedium.HARDCOPY );
		libraryService.createCopyInLibrary( suburbanLibrary1, indonesianEconomy, BookMedium.HARDCOPY );
		libraryService.createCopyInLibrary( suburbanLibrary1, javaForDummies, BookMedium.HARDCOPY );
		libraryService.createCopyInLibrary( suburbanLibrary1, artOfComputerProg, BookMedium.HARDCOPY );
		libraryService.createCopyInLibrary( suburbanLibrary1, thesaurusOfLanguages, BookMedium.HARDCOPY );

		// Suburban library 2
		Library suburbanLibrary2 = libraryService.create(
				LibraryShowcaseBaseIT.SUBURBAN_2_ID,
				"Suburban Library 2",
				800, // Same as the other suburban library
				42.0, -0.25,
				LibraryServiceOption.DISABLED_ACCESS, LibraryServiceOption.READING_ROOMS,
				LibraryServiceOption.HARDCOPY_LOAN
		);
		// Content: no academic document, offers dematerialized copies
		libraryService.createCopyInLibrary( suburbanLibrary2, calligraphy, BookMedium.HARDCOPY );
		libraryService.createCopyInLibrary( suburbanLibrary2, calligraphy, BookMedium.DEMATERIALIZED );
		libraryService.createCopyInLibrary( suburbanLibrary2, javaDancing, VideoMedium.DVD );
		libraryService.createCopyInLibrary( suburbanLibrary2, javaDancing, VideoMedium.DEMATERIALIZED );
		libraryService.createCopyInLibrary( suburbanLibrary2, javaForDummies, BookMedium.HARDCOPY );
		libraryService.createCopyInLibrary( suburbanLibrary2, javaForDummies, BookMedium.DEMATERIALIZED );
		libraryService.createCopyInLibrary( suburbanLibrary2, livingOnIsland, VideoMedium.BLURAY );
		libraryService.createCopyInLibrary( suburbanLibrary2, livingOnIsland, VideoMedium.DEMATERIALIZED );

		// University library
		Library universityLibrary = libraryService.create(
				LibraryShowcaseBaseIT.UNIVERSITY_ID,
				"University Library",
				9000,
				42.0, 0.5,
				LibraryServiceOption.READING_ROOMS,
				LibraryServiceOption.HARDCOPY_LOAN, LibraryServiceOption.DEMATERIALIZED_LOAN
		);
		// Content: only academic and learning documents
		libraryService.createCopyInLibrary( universityLibrary, indonesianEconomy, BookMedium.HARDCOPY );
		libraryService.createCopyInLibrary( universityLibrary, javaForDummies, BookMedium.HARDCOPY );
		libraryService.createCopyInLibrary( universityLibrary, artOfComputerProg, BookMedium.HARDCOPY );
		libraryService.createCopyInLibrary( universityLibrary, thesaurusOfLanguages, BookMedium.HARDCOPY );

		Person janeSmith = borrowalService.create( LibraryShowcaseBaseIT.JANE_SMITH_ID, "Jane", "Smith" );
		borrowalService.createAccount( janeSmith );
		borrowalService.borrow( janeSmith, cityCenterLibrary, indonesianEconomy, BorrowalType.SHORT_TERM );
		borrowalService.borrow( janeSmith, cityCenterLibrary, artOfComputerProg, BorrowalType.LONG_TERM );
		borrowalService.borrow( janeSmith, cityCenterLibrary, javaForDummies, BorrowalType.SHORT_TERM );
		borrowalService.borrow( janeSmith, cityCenterLibrary, calligraphy, BorrowalType.SHORT_TERM );
		borrowalService.borrow( janeSmith, cityCenterLibrary, javaDancing, BorrowalType.LONG_TERM );

		Person janeFonda = borrowalService.create( LibraryShowcaseBaseIT.JANE_FONDA_ID, "Jane", "Fonda" );
		borrowalService.createAccount( janeFonda );
		borrowalService.borrow( janeFonda, universityLibrary, javaForDummies, BorrowalType.SHORT_TERM );
		borrowalService.borrow( janeFonda, universityLibrary, artOfComputerProg, BorrowalType.SHORT_TERM );
		borrowalService.borrow( janeFonda, universityLibrary, thesaurusOfLanguages, BorrowalType.SHORT_TERM );
		borrowalService.borrow( janeFonda, universityLibrary, indonesianEconomy, BorrowalType.SHORT_TERM );

		// Use varying case on purpose
		Person janePorter = borrowalService.create( LibraryShowcaseBaseIT.JANE_PORTER_ID, "Jane", "porter" );
		borrowalService.createAccount( janePorter );
		borrowalService.borrow( janePorter, suburbanLibrary1, indonesianEconomy, BorrowalType.LONG_TERM );
		borrowalService.borrow( janePorter, suburbanLibrary2, livingOnIsland, 1, BorrowalType.LONG_TERM );
		borrowalService.borrow( janePorter, universityLibrary, thesaurusOfLanguages, BorrowalType.LONG_TERM );

		// Use varying case on purpose
		Person johnLennon = borrowalService.create( LibraryShowcaseBaseIT.JOHN_LENNON_ID, "john", "Lennon" );
		borrowalService.createAccount( johnLennon );
		borrowalService.borrow( johnLennon, suburbanLibrary2, javaDancing, 1, BorrowalType.SHORT_TERM );

		// Use varying case on purpose
		Person eltonJohn = borrowalService.create( LibraryShowcaseBaseIT.ELTON_JOHN_ID, "elton", "john" );
		borrowalService.createAccount( eltonJohn );
		borrowalService.borrow( eltonJohn, suburbanLibrary2, javaDancing, 1, BorrowalType.SHORT_TERM );

		Person pattySmith = borrowalService.create( LibraryShowcaseBaseIT.PATTY_SMITH_ID, "Patty", "Smith" );
		borrowalService.createAccount( pattySmith );
		borrowalService.borrow( pattySmith, suburbanLibrary2, javaDancing, 1, BorrowalType.SHORT_TERM );

		Person johnSmith = borrowalService.create( LibraryShowcaseBaseIT.JOHN_SMITH_ID, "John", "Smith" );
		borrowalService.createAccount( johnSmith );
		borrowalService.borrow( johnSmith, suburbanLibrary1, indonesianEconomy, BorrowalType.SHORT_TERM );
		borrowalService.borrow( johnSmith, suburbanLibrary1, artOfComputerProg, BorrowalType.LONG_TERM );

		@SuppressWarnings("unused")
		Person johnPaulSmith = borrowalService.create( LibraryShowcaseBaseIT.JOHN_PAUL_SMITH_ID, "John Paul", "Smith" );
		// No account for this one

		Person johnPaul = borrowalService.create( LibraryShowcaseBaseIT.JOHN_PAUL_ID, "John", "Paul" );
		borrowalService.createAccount( johnPaul );
		// This one has an account, but no borrowal

		Person paulJohn = borrowalService.create( LibraryShowcaseBaseIT.PAUL_JOHN_ID, "Paul", "John" );
		borrowalService.createAccount( paulJohn );
		borrowalService.borrow( paulJohn, cityCenterLibrary, javaForDummies, BorrowalType.SHORT_TERM );
		borrowalService.borrow( paulJohn, cityCenterLibrary, artOfComputerProg, BorrowalType.SHORT_TERM );

		assertThat( true ).isEqualTo( true );
	}

	public void initBooksDataSet(int numberOfBooks) {
		for ( int i = 0; i < numberOfBooks; i++ ) {
			String isbn = String.format( Locale.ROOT, "973-0-00-%06d-3", i );
			documentService.createBook( i, isbn, "Divine Comedy chapter n. " + ( i + 1 ), "Dante Alighieri " + ( i + 1 ),
					"The Divine Comedy is composed of 14,233 lines that are divided into three cantiche (singular cantica) – Inferno (Hell), Purgatorio (Purgatory), and Paradiso (Paradise)",
					"literature,poem,afterlife"
			);
		}
	}
}

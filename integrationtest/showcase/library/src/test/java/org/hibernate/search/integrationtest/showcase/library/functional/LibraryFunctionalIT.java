/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.functional;

import org.hibernate.search.integrationtest.showcase.library.TestActiveProfilesResolver;
import org.hibernate.search.integrationtest.showcase.library.model.Book;
import org.hibernate.search.integrationtest.showcase.library.model.BookMedium;
import org.hibernate.search.integrationtest.showcase.library.model.Library;
import org.hibernate.search.integrationtest.showcase.library.model.LibraryServiceOption;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

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
public class LibraryFunctionalIT {

	@Autowired
	private LessMemoryEntityService service;

	@Test
	@TestForIssue( jiraKey = "HSEARCH-1350" )
	public void test() {
		Book book = service.createBook( 739, "0-4206-9749-7", "The Late Mattia Pascal", "Luigi Pirandello", "Describes the human life conditions...", "misfortune" );
		Library cityCenterLibrary = service.createLibrary( 777, "The Western Library", 12400, 42.0, 0.0, LibraryServiceOption.READING_ROOMS, LibraryServiceOption.HARDCOPY_LOAN );

		service.createCopyInLibrary( cityCenterLibrary, book, BookMedium.HARDCOPY );
	}
}

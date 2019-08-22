/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.hibernate.search.integrationtest.showcase.library.model.Book;
import org.hibernate.search.integrationtest.showcase.library.service.DocumentService;
import org.hibernate.search.integrationtest.showcase.library.service.InnerTransactionRollbackService;
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
public class LibraryShowcaseTransactionIT {

	@Autowired
	private InnerTransactionRollbackService innerTransactionRollbackService;

	@Autowired
	private DocumentService documentService;

	@Test
	@TestForIssue( jiraKey = "HSEARCH-1270" )
	public void innerTransactionRollback() {
		String outerIsbn = "0-4206-9749-7";
		String innerIsbn = "0-1111-9749-7";

		innerTransactionRollbackService.doOuter( outerIsbn, innerIsbn );

		Optional<Book> outerBook = documentService.getByIsbn( outerIsbn );
		Optional<Book> innerBook = documentService.getByIsbn( innerIsbn );

		// Check that outer transaction data has been pushed to the index:
		assertTrue( outerBook.isPresent() );

		// Check that inner transaction data has NOT been pushed to the index:
		assertFalse( innerBook.isPresent() );
	}

	@Test
	@TestForIssue( jiraKey = "HSEARCH-1270" )
	public void innerTransactionRollback_flushBeforeInner() {
		String outerIsbn = "0-4206-9749-7";
		String innerIsbn = "0-1111-9749-7";

		innerTransactionRollbackService.doOuterFlushBeforeInner( outerIsbn, innerIsbn );

		Optional<Book> outerBook = documentService.getByIsbn( outerIsbn );
		Optional<Book> innerBook = documentService.getByIsbn( innerIsbn );

		// Check that outer transaction data has been pushed to the index:
		assertTrue( outerBook.isPresent() );

		// Check that inner transaction data has NOT been pushed to the index:
		assertFalse( innerBook.isPresent() );
	}
}
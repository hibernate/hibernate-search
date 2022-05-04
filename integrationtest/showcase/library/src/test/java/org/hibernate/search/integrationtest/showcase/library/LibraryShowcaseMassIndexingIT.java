/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import org.hibernate.search.integrationtest.showcase.library.service.AdminService;
import org.hibernate.search.integrationtest.showcase.library.service.DocumentService;
import org.hibernate.search.integrationtest.showcase.library.service.TestDataService;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.logging.log4j.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
		"spring.jpa.properties.hibernate.search.automatic_indexing.enabled=false"
})
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
public class LibraryShowcaseMassIndexingIT {

	private static final int NUMBER_OF_BOOKS = 200;
	private static final int MASS_INDEXING_MONITOR_LOG_PERIOD = 50; // This is the default in the implementation, do not change this value
	static {
		checkInvariants();
	}

	@SuppressWarnings("unused")
	private static void checkInvariants() {
		if ( NUMBER_OF_BOOKS < 2 * MASS_INDEXING_MONITOR_LOG_PERIOD ) {
			throw new IllegalStateException(
					"There's a bug in tests: NUMBER_OF_BOOKS should be strictly higher than two times "
							+ MASS_INDEXING_MONITOR_LOG_PERIOD
			);
		}
	}

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Autowired
	private DocumentService documentService;

	@Autowired
	private AdminService adminService;

	@Autowired
	private TestDataService testDataService;

	@Before
	public void initData() {
		testDataService.initBooksDataSet( NUMBER_OF_BOOKS );
	}

	@After
	public void cleanUpData() {
		// we're cleaning the data manually,
		// in order to have a class level application context,
		// to support the job of ExpectedLog4jLog
		documentService.purge();
	}

	@Test
	public void testMassIndexing() {
		assertThat( documentService.countIndexed() ).isEqualTo( 0 );
		MassIndexer indexer = adminService.createMassIndexer();
		try {
			indexer.startAndWait();
		}
		catch (InterruptedException e) {
			fail( "Unexpected InterruptedException: " + e.getMessage() );
		}
		assertThat( documentService.countIndexed() ).isEqualTo( NUMBER_OF_BOOKS );
	}

	@Test
	public void testMassIndexingMonitor() {
		assertThat( documentService.countIndexed() ).isEqualTo( 0 );
		MassIndexer indexer = adminService.createMassIndexer();
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

			// Example:
			// Mass indexing progress: indexed 151 entities in 21 ms.
			logged.expectEvent( Level.INFO, "Mass indexing progress: indexed", "entities in", "ms" ).times( expectedNumberOfLogs );

			// Example:
			// Mass indexing progress: 26.50%. Mass indexing speed: 2765.605713 documents/second since last message, 2765.605713 documents/second since start.
			logged.expectEvent( Level.INFO, "Mass indexing progress:", "%", "Mass indexing speed:", "documents/second since last message", "documents/second since start" ).times( expectedNumberOfLogs );

			indexer.startAndWait();
		}
		catch (InterruptedException e) {
			fail( "Unexpected InterruptedException: " + e.getMessage() );
		}
		assertThat( documentService.countIndexed() ).isEqualTo( NUMBER_OF_BOOKS );
	}

}

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

import org.junit.After;
import org.junit.Before;
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
@TestPropertySource(properties = {
		"spring.jpa.properties.hibernate.search.automatic_indexing.strategy=none",
		"spring.jpa.properties.hibernate.search.schema_management.strategy=none"
})
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class LibraryShowcaseMassIndexingManualSchemaManagementIT {

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

	@Before
	@After
	public void cleanup() {
		// Necessary to keep the server (ES) or filesystem (Lucene) clean after the tests,
		// because the schema management strategy is "none"
		adminService.dropSchema();
	}

	@Test
	public void testMassIndexingWithAutomaticDropAndCreate() {
		// The index doesn't exist initially, since we delete it in "cleanup()" the schema management strategy is "none"
		MassIndexer indexer = adminService.createMassIndexer()
				.dropAndCreateSchemaOnStart( true );
		try {
			indexer.startAndWait();
		}
		catch (InterruptedException e) {
			fail( "Unexpected InterruptedException: " + e.getMessage() );
		}
		assertThat( documentService.countIndexed() ).isEqualTo( NUMBER_OF_BOOKS );
	}

	@Test
	public void testMassIndexingWithManualDropAndCreate() {
		// The index doesn't exist initially, since the schema management strategy is "none"
		adminService.dropAndCreateSchema();
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
}

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
		"spring.jpa.properties.hibernate.search.indexing.listeners.enabled=false"
})
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
public class LibraryShowcaseMassIndexingIT {

	private static final int NUMBER_OF_BOOKS = 200;

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
		assertThat( documentService.countIndexed() ).isZero();
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

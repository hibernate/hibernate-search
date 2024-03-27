/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import org.hibernate.search.integrationtest.showcase.library.service.AdminService;
import org.hibernate.search.integrationtest.showcase.library.service.DocumentService;
import org.hibernate.search.integrationtest.showcase.library.service.TestDataService;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
		"spring.jpa.properties.hibernate.search.indexing.listeners.enabled=false"
})
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
class LibraryShowcaseMassIndexingIT extends AbstractLibraryShowcaseSearchIT {

	private static final int NUMBER_OF_BOOKS = 200;

	@Autowired
	private DocumentService documentService;

	@Autowired
	private AdminService adminService;

	@Autowired
	private TestDataService testDataService;

	@BeforeEach
	void initData() {
		testDataService.initBooksDataSet( NUMBER_OF_BOOKS );
	}

	@AfterEach
	void cleanUpData() {
		// we're cleaning the data manually,
		// in order to have a class level application context,
		// to support the job of ExpectedLog4jLog
		documentService.purge();
	}

	@Test
	void testMassIndexing() {
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

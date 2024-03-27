/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.CITY_CENTER_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.SUBURBAN_1_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.SUBURBAN_2_ID;
import static org.hibernate.search.integrationtest.showcase.library.service.TestDataService.UNIVERSITY_ID;

import java.util.List;

import org.hibernate.search.integrationtest.showcase.library.dto.LibrarySimpleProjectionRecord;
import org.hibernate.search.integrationtest.showcase.library.model.Library;
import org.hibernate.search.integrationtest.showcase.library.service.Java17Service;
import org.hibernate.search.integrationtest.showcase.library.service.LibraryService;
import org.hibernate.search.integrationtest.showcase.library.service.TestDataService;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class Java17IT extends AbstractLibraryShowcaseSearchIT{

	private static boolean needsInit;

	@Autowired
	private LibraryService libraryService;

	@Autowired
	private Java17Service java17Service;

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

	// This checks that top-level records get automatically indexed by Hibernate Search with Jandex, in particular.
	@Test
	void searchAndProject() {
		List<LibrarySimpleProjectionRecord> libraries = java17Service.searchAndProjectToRecord( "library", 0, 10 );
		assertThat( libraries ).extracting( LibrarySimpleProjectionRecord::name ).containsExactly(
				libraryService.getById( CITY_CENTER_ID, Library::getName ),
				libraryService.getById( UNIVERSITY_ID, Library::getName ),
				libraryService.getById( SUBURBAN_1_ID, Library::getName ),
				libraryService.getById( SUBURBAN_2_ID, Library::getName ) );
		assertThat( libraries ).extracting( LibrarySimpleProjectionRecord::services ).containsExactly(
				libraryService.getById( CITY_CENTER_ID, Library::getServices ),
				libraryService.getById( UNIVERSITY_ID, Library::getServices ),
				libraryService.getById( SUBURBAN_1_ID, Library::getServices ),
				libraryService.getById( SUBURBAN_2_ID, Library::getServices ) );

		libraries = java17Service.searchAndProjectToRecord( "library", 1, 2 );
		assertThat( libraries ).extracting( LibrarySimpleProjectionRecord::name ).containsExactly(
				libraryService.getById( UNIVERSITY_ID, Library::getName ),
				libraryService.getById( SUBURBAN_1_ID, Library::getName ) );
		assertThat( libraries ).extracting( LibrarySimpleProjectionRecord::services ).containsExactly(
				libraryService.getById( UNIVERSITY_ID, Library::getServices ),
				libraryService.getById( SUBURBAN_1_ID, Library::getServices ) );

		libraries = java17Service.searchAndProjectToRecord( "sUburban", 0, 10 );
		assertThat( libraries ).extracting( LibrarySimpleProjectionRecord::name ).containsExactly(
				libraryService.getById( SUBURBAN_1_ID, Library::getName ),
				libraryService.getById( SUBURBAN_2_ID, Library::getName ) );
		assertThat( libraries ).extracting( LibrarySimpleProjectionRecord::services ).containsExactly(
				libraryService.getById( SUBURBAN_1_ID, Library::getServices ),
				libraryService.getById( SUBURBAN_2_ID, Library::getServices ) );
	}

	// This checks that method-local records get automatically indexed by Hibernate Search with Jandex, in particular.
	@Test
	void searchAndProjectToMethodLocalClass() {
		List<LibrarySimpleProjectionRecord> libraries = java17Service.searchAndProjectToMethodLocalRecord( "library", 0, 10 );
		assertThat( libraries ).extracting( LibrarySimpleProjectionRecord::name ).containsExactly(
				libraryService.getById( CITY_CENTER_ID, Library::getName ),
				libraryService.getById( UNIVERSITY_ID, Library::getName ),
				libraryService.getById( SUBURBAN_1_ID, Library::getName ),
				libraryService.getById( SUBURBAN_2_ID, Library::getName ) );
		assertThat( libraries ).extracting( LibrarySimpleProjectionRecord::services ).containsExactly(
				libraryService.getById( CITY_CENTER_ID, Library::getServices ),
				libraryService.getById( UNIVERSITY_ID, Library::getServices ),
				libraryService.getById( SUBURBAN_1_ID, Library::getServices ),
				libraryService.getById( SUBURBAN_2_ID, Library::getServices ) );

		libraries = java17Service.searchAndProjectToMethodLocalRecord( "library", 1, 2 );
		assertThat( libraries ).extracting( LibrarySimpleProjectionRecord::name ).containsExactly(
				libraryService.getById( UNIVERSITY_ID, Library::getName ),
				libraryService.getById( SUBURBAN_1_ID, Library::getName ) );
		assertThat( libraries ).extracting( LibrarySimpleProjectionRecord::services ).containsExactly(
				libraryService.getById( UNIVERSITY_ID, Library::getServices ),
				libraryService.getById( SUBURBAN_1_ID, Library::getServices ) );

		libraries = java17Service.searchAndProjectToMethodLocalRecord( "sUburban", 0, 10 );
		assertThat( libraries ).extracting( LibrarySimpleProjectionRecord::name ).containsExactly(
				libraryService.getById( SUBURBAN_1_ID, Library::getName ),
				libraryService.getById( SUBURBAN_2_ID, Library::getName ) );
		assertThat( libraries ).extracting( LibrarySimpleProjectionRecord::services ).containsExactly(
				libraryService.getById( SUBURBAN_1_ID, Library::getServices ),
				libraryService.getById( SUBURBAN_2_ID, Library::getServices ) );
	}

}

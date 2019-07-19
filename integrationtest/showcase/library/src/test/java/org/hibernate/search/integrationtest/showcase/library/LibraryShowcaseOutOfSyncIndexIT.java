/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.integrationtest.showcase.library.service.DocumentService;
import org.hibernate.search.integrationtest.showcase.library.service.TestDataService;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test behavior when the index becomes out of sync with the database.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = { "automatic_indexing.strategy=session" })
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class LibraryShowcaseOutOfSyncIndexIT {

	@Autowired
	private DocumentService documentService;

	@Autowired
	private TestDataService testDataService;

	@Before
	public void before() {
		testDataService.initDefaultDataSet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void search_skipDeletedEntitiesInHits() {
		// Check that document counts are identical
		long entityCount = documentService.count();
		long indexedEntityCount = documentService.countIndexed();
		assertThat( entityCount ).isGreaterThan( 0 );
		assertThat( indexedEntityCount ).isEqualTo( entityCount );

		// Simulate an external delete that Hibernate Search will not be able to detect
		testDataService.executeHql(
				"DELETE FROM Book WHERE MOD(id, 2) = 0"
		);

		// Check that document counts are off
		entityCount = documentService.count();
		indexedEntityCount = documentService.countIndexed();
		assertThat( entityCount ).isGreaterThan( 0 );
		assertThat( indexedEntityCount ).isGreaterThan( entityCount );

		// Check that running a search query still returns the correct number of hits,
		// because hits that cannot be loaded are ignored
		assertThat( documentService.findAllIndexed() )
				.hasSize( (int) entityCount )
				.doesNotContainNull();
	}

}

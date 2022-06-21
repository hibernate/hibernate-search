/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexer;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

/**
 * Tests of individual operations in {@link org.hibernate.search.mapper.pojo.work.spi.PojoIndexer}
 * when the entity passed to the operation is null.
 */
@TestForIssue(jiraKey = "HSEARCH-4153")
public abstract class AbstractPojoIndexerAddOrUpdateNullEntityIT extends AbstractPojoIndexingOperationIT {

	@Test
	public void simple() {
		try ( SearchSession session = createSession() ) {
			SearchIndexer indexer = session.indexer();

			assertThatThrownBy( () -> scenario().execute( indexer, 42 ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid indexing request",
							"the add and update operations require a non-null entity" );
		}
	}

	@Override
	protected boolean isImplicitRoutingEnabled() {
		// Entities are null and are not implicitly loaded, so implicit routing simply cannot work.
		return false;
	}

}

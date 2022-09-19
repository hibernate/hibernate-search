/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.massindexing;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.hibernate.search.integrationtest.mapper.orm.realbackend.util.BookCreatorUtils.prepareBooks;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.integrationtest.mapper.orm.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.integrationtest.mapper.orm.realbackend.util.Book;
import org.hibernate.search.integrationtest.mapper.orm.realbackend.util.BookCreatorUtils;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MassIndexingManualSchemaManagementIT {

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
	public OrmSetupHelper setupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void before() {
		entityManagerFactory = setupHelper.start()
				.withProperty( "hibernate.search.automatic_indexing.enabled", false )
				.withProperty( "hibernate.search.schema_management.strategy", "none" )
				.setup( Book.class );

		prepareBooks( entityManagerFactory, NUMBER_OF_BOOKS );
		cleanup();
	}

	@After
	public void cleanup() {
		// Necessary to keep the server (ES) or filesystem (Lucene) clean after the tests,
		// because the schema management strategy is "none"
		with( entityManagerFactory ).runInTransaction( entityManager ->
				Search.session( entityManager ).schemaManager().dropIfExisting()
		);
	}

	@Test
	public void testMassIndexingWithAutomaticDropAndCreate() {
		// The index doesn't exist initially, since we delete it in "cleanup()" the schema management strategy is "none"
		with( entityManagerFactory ).runInTransaction( entityManager -> {
					MassIndexer indexer = Search.session( entityManager ).massIndexer()
							.dropAndCreateSchemaOnStart( true );
					try {
						indexer.startAndWait();
					}
					catch (InterruptedException e) {
						fail( "Unexpected InterruptedException: " + e.getMessage() );
					}
					assertThat( BookCreatorUtils.documentsCount( entityManagerFactory ) ).isEqualTo( NUMBER_OF_BOOKS );
				}
		);
	}

	@Test
	public void testMassIndexingWithManualDropAndCreate() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
					// The index doesn't exist initially, since the schema management strategy is "none"
					Search.session( entityManager ).schemaManager().dropAndCreate();

					assertThat( BookCreatorUtils.documentsCount( entityManagerFactory ) ).isZero();

					MassIndexer indexer = Search.session( entityManager ).massIndexer()
							.dropAndCreateSchemaOnStart( true );
					try {
						indexer.startAndWait();
					}
					catch (InterruptedException e) {
						fail( "Unexpected InterruptedException: " + e.getMessage() );
					}
					assertThat( BookCreatorUtils.documentsCount( entityManagerFactory ) ).isEqualTo( NUMBER_OF_BOOKS );
				}
		);
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.directory;

import java.io.IOException;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.backend.lucene.index.impl.LuceneIndexManagerImpl;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.Lock;
import org.assertj.core.api.Assertions;

public abstract class AbstractBuiltInDirectoryIT extends AbstractDirectoryIT {

	protected static final String SINGLE_INSTANCE_LOCK_FQN = "org.apache.lucene.store.SingleInstanceLockFactory$SingleInstanceLock";
	protected static final String SIMPLE_FS_LOCK_FQN = "org.apache.lucene.store.SimpleFSLockFactory$SimpleFSLock";
	protected static final String NATIVE_FS_LOCK_FQN = "org.apache.lucene.store.NativeFSLockFactory$NativeFSLock";
	protected static final String NO_LOCK_FQN = "org.apache.lucene.store.NoLockFactory$NoLock";

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	@PortedFromSearch5(original = "org.hibernate.search.test.directoryProvider.CustomLockProviderTest.testUseOfNativeLockingFactory")
	public void lockingStrategy_default() throws IOException {
		testLockingStrategy( null, getDefaultLockClassName() );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	@PortedFromSearch5(original = "org.hibernate.search.test.directoryProvider.CustomLockProviderTest.testUseOfSimpleLockingFactory")
	public void lockingStrategy_simpleFilesystem() throws IOException {
		testLockingStrategy( "simple-filesystem", SIMPLE_FS_LOCK_FQN );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	@PortedFromSearch5(original = "org.hibernate.search.test.directoryProvider.CustomLockProviderTest.testUseOfNativeLockingFactory")
	public void lockingStrategy_nativeFilesystem() throws IOException {
		testLockingStrategy( "native-filesystem", NATIVE_FS_LOCK_FQN );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	@PortedFromSearch5(original = "org.hibernate.search.test.directoryProvider.CustomLockProviderTest.testUseOfSingleLockingFactory")
	public void lockingStrategy_singleInstance() throws IOException {
		testLockingStrategy( "single-instance", SINGLE_INSTANCE_LOCK_FQN );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	public void lockingStrategy_none() throws IOException {
		testLockingStrategy( "none", NO_LOCK_FQN );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	@PortedFromSearch5(original = "org.hibernate.search.test.directoryProvider.CustomLockProviderTest.testFailOnNonExistentLockingFactory")
	public void lockingStrategy_invalid() throws IOException {
		SubTest.expectException( () -> setup( c -> c.withBackendProperty(
				BACKEND_NAME, LuceneBackendSettings.DIRECTORY_LOCKING_STRATEGY,
				"some_invalid_name"
		) ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.backendContext( BACKEND_NAME )
						.failure(
								"Invalid locking strategy name",
								"'some_invalid_name'",
								"Valid names are: [simple-filesystem, native-filesystem, single-instance, none]"
						)
						.build()
				);
		testLockingStrategy( "none", NO_LOCK_FQN );
	}

	protected abstract Object getDirectoryType();

	protected abstract String getDefaultLockClassName();

	protected final void setup(
			Function<SearchSetupHelper.SetupContext, SearchSetupHelper.SetupContext> additionalConfiguration) {
		setup( getDirectoryType(), additionalConfiguration );
	}

	private void testLockingStrategy(String strategyName, String expectedLockClassName) throws IOException {
		setup( c -> {
			if ( strategyName != null ) {
				c.withBackendProperty(
						BACKEND_NAME, LuceneBackendSettings.DIRECTORY_LOCKING_STRATEGY,
						strategyName
				);
			}
			return c;
		} );

		checkIndexingAndQuerying();

		LuceneIndexManagerImpl luceneIndexManager = indexManager.unwrapForTests( LuceneIndexManagerImpl.class );
		Directory directory = luceneIndexManager.getIndexAccessorForTests().getDirectoryForTests();
		try ( Lock lock = directory.obtainLock( "my-lock" ) ) {
			Assertions.assertThat( lock.getClass().getName() )
					.isEqualTo( expectedLockClassName );
		}
	}
}

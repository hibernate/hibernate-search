/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.backend.lucene.index.impl.LuceneIndexManagerImpl;
import org.hibernate.search.backend.lucene.index.impl.Shard;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessorImpl;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

import org.apache.lucene.store.Lock;

public abstract class AbstractBuiltInDirectoryIT extends AbstractDirectoryIT {

	protected static final String SINGLE_INSTANCE_LOCK_FQN =
			"org.apache.lucene.store.SingleInstanceLockFactory$SingleInstanceLock";
	protected static final String SIMPLE_FS_LOCK_FQN = "org.apache.lucene.store.SimpleFSLockFactory$SimpleFSLock";
	protected static final String NATIVE_FS_LOCK_FQN = "org.apache.lucene.store.NativeFSLockFactory$NativeFSLock";
	protected static final String NO_LOCK_FQN = "org.apache.lucene.store.NoLockFactory$NoLock";

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	@PortedFromSearch5(
			original = "org.hibernate.search.test.directoryProvider.CustomLockProviderTest.testUseOfNativeLockingFactory")
	public void lockingStrategy_default() {
		testValidLockingStrategy( null, getDefaultLockClassName() );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	@PortedFromSearch5(
			original = "org.hibernate.search.test.directoryProvider.CustomLockProviderTest.testUseOfSimpleLockingFactory")
	public void lockingStrategy_simpleFilesystem() {
		if ( isFSDirectory() ) {
			testValidLockingStrategy( "simple-filesystem", SIMPLE_FS_LOCK_FQN );
		}
		else {
			testInvalidFSLockingStrategy( "simple-filesystem" );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	@PortedFromSearch5(
			original = "org.hibernate.search.test.directoryProvider.CustomLockProviderTest.testUseOfNativeLockingFactory")
	public void lockingStrategy_nativeFilesystem() {
		if ( isFSDirectory() ) {
			testValidLockingStrategy( "native-filesystem", NATIVE_FS_LOCK_FQN );
		}
		else {
			testInvalidFSLockingStrategy( "simple-filesystem" );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	@PortedFromSearch5(
			original = "org.hibernate.search.test.directoryProvider.CustomLockProviderTest.testUseOfSingleLockingFactory")
	public void lockingStrategy_singleInstance() {
		testValidLockingStrategy( "single-instance", SINGLE_INSTANCE_LOCK_FQN );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	public void lockingStrategy_none() {
		testValidLockingStrategy( "none", NO_LOCK_FQN );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	@PortedFromSearch5(
			original = "org.hibernate.search.test.directoryProvider.CustomLockProviderTest.testFailOnNonExistentLockingFactory")
	public void lockingStrategy_invalid() {
		assertThatThrownBy( () -> setup( c -> c.withBackendProperty(
				LuceneIndexSettings.DIRECTORY_LOCKING_STRATEGY,
				"some_invalid_name"
		) ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.indexContext( index.name() )
						.failure(
								"Invalid locking strategy name",
								"'some_invalid_name'",
								"Valid names are: [simple-filesystem, native-filesystem, single-instance, none]"
						)
				);
		testValidLockingStrategy( "none", NO_LOCK_FQN );
	}

	protected abstract Object getDirectoryType();

	protected abstract boolean isFSDirectory();

	protected abstract String getDefaultLockClassName();

	protected final void setup(
			Function<SearchSetupHelper.SetupContext, SearchSetupHelper.SetupContext> additionalConfiguration) {
		setup( getDirectoryType(), additionalConfiguration );
	}

	private void testValidLockingStrategy(String strategyName, String expectedLockClassName) {
		setup( c -> {
			if ( strategyName != null ) {
				c.withBackendProperty(
						LuceneIndexSettings.DIRECTORY_LOCKING_STRATEGY,
						strategyName
				);
			}
			return c;
		} );

		checkIndexingAndQuerying();

		LuceneIndexManagerImpl luceneIndexManager = index.unwrapForTests( LuceneIndexManagerImpl.class );
		assertThat( luceneIndexManager.getShardsForTests() )
				.extracting( Shard::indexAccessorForTests )
				.extracting( IndexAccessorImpl::getDirectoryForTests )
				.isNotEmpty()
				.allSatisfy( directory -> {
					try ( Lock lock = directory.obtainLock( "my-lock" ) ) {
						assertThat( lock.getClass().getName() )
								.isEqualTo( expectedLockClassName );
					}
					catch (IOException e) {
						throw new IllegalStateException( "Unexpected exception during test: " + e.getMessage(), e );
					}
				} );
	}

	private void testInvalidFSLockingStrategy(String strategyName) {
		assertThatThrownBy( () -> setup( c -> c.withBackendProperty(
				LuceneIndexSettings.DIRECTORY_LOCKING_STRATEGY,
				strategyName
		) ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.indexContext( index.name() )
						.failure(
								"Unable to initialize index directory",
								"can only be used with FSDirectory subclasses"
						)
				);
	}

}

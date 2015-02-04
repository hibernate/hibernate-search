/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.reader.functionality;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.hibernate.search.indexes.impl.SharingBufferReaderProvider;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.impl.RAMDirectoryProvider;
import org.hibernate.search.testsupport.setup.BuildContextForTest;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;

/**
 * Testable extension of SharingBufferReaderProvider to make sure IndexReaders
 * are only opened when needed, and always correctly closed.
 *
 * @author Sanne Grinovero
 * @see SharingBufferIndexProviderTest
 */
public class ExtendedSharingBufferReaderProvider extends SharingBufferReaderProvider {

	private static final int NUM_DIRECTORY_PROVIDERS = 3;
	private final Vector<MockIndexReader> createdReadersHistory = new Vector<MockIndexReader>( 500 );
	final Map<Directory, TestManipulatorPerDP> manipulators = new ConcurrentHashMap<Directory, TestManipulatorPerDP>();
	private final RAMDirectoryProvider[] directories = new RAMDirectoryProvider[NUM_DIRECTORY_PROVIDERS];
	private final AtomicInteger currentDirectoryIndex = new AtomicInteger();
	private volatile RAMDirectoryProvider currentDirectory;

	public ExtendedSharingBufferReaderProvider() {
		for ( int i = 0; i < NUM_DIRECTORY_PROVIDERS; i++ ) {
			TestManipulatorPerDP tm = new TestManipulatorPerDP( i );
			manipulators.put( tm.dp.getDirectory(), tm );
			directories[i] = tm.dp;
		}
		currentDirectory = directories[0];
	}

	/**
	 * Contains mutable state related to a specific index
	 */
	public static class TestManipulatorPerDP {
		private final AtomicBoolean isIndexReaderCurrent = new AtomicBoolean( false );//starts at true, see MockIndexReader constructor
		private final AtomicBoolean isReaderCreated = new AtomicBoolean( false );
		private final RAMDirectoryProvider dp = new RAMDirectoryProvider();

		TestManipulatorPerDP(int seed) {
			dp.initialize( String.valueOf( seed ), null, new BuildContextForTest( new SearchConfigurationForTest() ) );
		}

		public void setIndexChanged() {
			isIndexReaderCurrent.set( false );
		}
	}

	/**
	 * Make the last IndexReader opened on the current Directory dirty
	 */
	public void currentDPWasWritten() {
		for ( TestManipulatorPerDP manipulator : manipulators.values() ) {
			manipulator.setIndexChanged();
		}
	}

	/**
	 * Switches the current Directory, what is going to be returned by the mock DirectoryProvider
	 */
	public void swithDirectory() {
		int index = currentDirectoryIndex.incrementAndGet();
		currentDirectory = directories[index % NUM_DIRECTORY_PROVIDERS];
	}

	public boolean isReaderCurrent(MockIndexReader reader) {
		//avoid usage of allReaders or test would be useless
		for ( PerDirectoryLatestReader latest : currentReaders.values() ) {
			IndexReader latestReader = latest.current.reader;
			if ( latestReader == reader ) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected DirectoryReader readerFactory(Directory directory) {
		TestManipulatorPerDP manipulatorPerDP = manipulators.get( directory );
		if ( !manipulatorPerDP.isReaderCreated.compareAndSet( false, true ) ) {
			throw new IllegalStateException( "IndexReader created twice" );
		}
		else {
			return new MockIndexReader( manipulatorPerDP.isIndexReaderCurrent );
		}
	}

	public void initialize() {
		super.initialize( new MockDirectoryBasedIndexManager(), null );
	}

	public boolean areAllOldReferencesGone() {
		int numReferencesReaders = allReaders.size();
		int numExpectedActiveReaders = manipulators.size();
		return numReferencesReaders == numExpectedActiveReaders;
	}

	public List<MockIndexReader> getCreatedIndexReaders() {
		return createdReadersHistory;
	}

	/**
	 * Use our special DirectoryProvider to emulate index switching and dirtyness.
	 */
	public class MockDirectoryBasedIndexManager extends DirectoryBasedIndexManager {

		private MockDirectoryProvider provider = new MockDirectoryProvider();

		@Override
		public DirectoryProvider getDirectoryProvider() {
			return provider;
		}

	}

	public class MockDirectoryProvider implements DirectoryProvider<RAMDirectory> {

		@Override
		public void initialize(String directoryProviderName, Properties properties, BuildContext context) {
		}

		@Override
		public void start(DirectoryBasedIndexManager indexManager) {
		}

		@Override
		public void stop() {
		}

		@Override
		public RAMDirectory getDirectory() {
			return currentDirectory.getDirectory();
		}
	}

	public class MockIndexReader extends DirectoryReader {

		private final AtomicBoolean closed = new AtomicBoolean( false );
		private final AtomicBoolean hasAlreadyBeenReOpened = new AtomicBoolean( false );
		private final AtomicBoolean isIndexReaderCurrent;

		MockIndexReader(AtomicBoolean isIndexReaderCurrent) {
			//make the super constructor happy as the class is "locked down"
			super( new RAMDirectory(), new AtomicReader[0] );
			this.isIndexReaderCurrent = isIndexReaderCurrent;
			if ( !isIndexReaderCurrent.compareAndSet( false, true ) ) {
				throw new IllegalStateException( "Unnecessarily reopened" );
			}
			createdReadersHistory.add( this );
		}

		public final boolean isClosed() {
			return closed.get();
		}

		@Override
		protected void doClose() throws IOException {
			boolean okToClose = closed.compareAndSet( false, true );
			if ( !okToClose ) {
				throw new IllegalStateException( "Attempt to close a closed IndexReader" );
			}
			if ( !hasAlreadyBeenReOpened.get() ) {
				throw new IllegalStateException( "Attempt to close the most current IndexReader" );
			}
		}

		@Override
		public boolean hasDeletions() {
			return false;//just something to make MultiReader constructor happy
		}

		@Override
		protected DirectoryReader doOpenIfChanged() throws IOException {
			if ( isIndexReaderCurrent.get() ) {
				return null;
			}
			else {
				if ( hasAlreadyBeenReOpened.compareAndSet( false, true ) ) {
					return new MockIndexReader( isIndexReaderCurrent );
				}
				else {
					throw new IllegalStateException( "Attempt to reopen an old IndexReader more than once" );
				}
			}
		}

		@Override
		protected DirectoryReader doOpenIfChanged(IndexCommit commit) throws IOException {
			return doOpenIfChanged();
		}

		@Override
		protected DirectoryReader doOpenIfChanged(IndexWriter writer, boolean applyAllDeletes) throws IOException {
			return doOpenIfChanged();
		}

		@Override
		public long getVersion() {
			return 0;
		}

		@Override
		public boolean isCurrent() throws IOException {
			return false;
		}

		@Override
		public IndexCommit getIndexCommit() throws IOException {
			return null;
		}

	}

}

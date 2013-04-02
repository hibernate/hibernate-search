/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.index.TermVectorMapper;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.impl.SharingBufferReaderProvider;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.impl.RAMDirectoryProvider;

/**
 * Testable extension of SharingBufferReaderProvider to make sure IndexReaders
 * are only opened when needed, and always correctly closed.
 *
 * @see SharingBufferIndexProviderTest
 * @author Sanne Grinovero
 */
public class ExtendedSharingBufferReaderProvider extends SharingBufferReaderProvider {

	private static final int NUM_DIRECTORY_PROVIDERS = 3;
	private final Vector<MockIndexReader> createdReadersHistory = new Vector<MockIndexReader>( 500 );
	final Map<Directory, TestManipulatorPerDP> manipulators = new ConcurrentHashMap<Directory, TestManipulatorPerDP>();
	private final RAMDirectoryProvider[] directories = new RAMDirectoryProvider[ NUM_DIRECTORY_PROVIDERS ];
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
			dp.initialize( String.valueOf( seed ), null, null );
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
		currentDirectory = directories[ index % NUM_DIRECTORY_PROVIDERS ];
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
	protected IndexReader readerFactory(Directory directory) {
		TestManipulatorPerDP manipulatorPerDP = manipulators.get( directory );
		if ( !manipulatorPerDP.isReaderCreated.compareAndSet( false, true ) ) {
			throw new IllegalStateException( "IndexReader created twice" );
		}
		else {
			return new MockIndexReader( manipulatorPerDP.isIndexReaderCurrent );
		}
	}

	@Override
	public void initialize(DirectoryBasedIndexManager indexManager, Properties props) {
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

	public class MockIndexReader extends IndexReader {

		private final AtomicBoolean closed = new AtomicBoolean( false );
		private final AtomicBoolean hasAlreadyBeenReOpened = new AtomicBoolean( false );
		private final AtomicBoolean isIndexReaderCurrent;

		MockIndexReader(AtomicBoolean isIndexReaderCurrent) {
			this.isIndexReaderCurrent = isIndexReaderCurrent;
			if ( ! isIndexReaderCurrent.compareAndSet( false, true ) ) {
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
		public synchronized IndexReader reopen() {
			if ( isIndexReaderCurrent.get() ) {
				return this;
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
		protected void doDelete(int docNum) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void doSetNorm(int doc, String field, byte value) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void doUndeleteAll() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int docFreq(Term t) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Document document(int n, FieldSelector fieldSelector) {
			throw new UnsupportedOperationException();
		}

		@Override
		public TermFreqVector getTermFreqVector(int docNumber, String field) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void getTermFreqVector(int docNumber, String field, TermVectorMapper mapper) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void getTermFreqVector(int docNumber, TermVectorMapper mapper) {
			throw new UnsupportedOperationException();
		}

		@Override
		public TermFreqVector[] getTermFreqVectors(int docNumber) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasDeletions() {
			return false;//just something to make MultiReader constructor happy
		}

		@Override
		public boolean isDeleted(int n) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int maxDoc() {
			return 10;//just something to make MultiReader constructor happy
		}

		@Override
		public byte[] norms(String field) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void norms(String field, byte[] bytes, int offset) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int numDocs() {
			throw new UnsupportedOperationException();
		}

		@Override
		public TermDocs termDocs() {
			throw new UnsupportedOperationException();
		}

		@Override
		public TermPositions termPositions() {
			throw new UnsupportedOperationException();
		}

		@Override
		public TermEnum terms() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public TermEnum terms(Term t) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void doCommit(Map<String, String> commitUserData) {
			// no-op
		}

		@Override
		public FieldInfos getFieldInfos() {
			throw new UnsupportedOperationException();
		}

	}

}

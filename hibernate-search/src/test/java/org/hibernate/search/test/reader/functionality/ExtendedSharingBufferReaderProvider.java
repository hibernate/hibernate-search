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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.index.TermVectorMapper;
import org.apache.lucene.store.Directory;

import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.SearchException;
import org.hibernate.search.reader.ReaderProviderHelper;
import org.hibernate.search.reader.SharingBufferReaderProvider;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.RAMDirectoryProvider;

/**
 * @author Sanne Grinovero
 */
public class ExtendedSharingBufferReaderProvider extends SharingBufferReaderProvider {

	private static final int NUM_DIRECTORY_PROVIDERS = 4;
	private final Vector<MockIndexReader> createdReadersHistory = new Vector<MockIndexReader>( 500 );
	final Map<Directory, TestManipulatorPerDP> manipulators = new ConcurrentHashMap<Directory, TestManipulatorPerDP>();
	final List<DirectoryProvider> directoryProviders = Collections.synchronizedList(new ArrayList<DirectoryProvider>());
	
	public ExtendedSharingBufferReaderProvider() {
		for ( int i = 0; i < NUM_DIRECTORY_PROVIDERS; i++ ) {
			TestManipulatorPerDP tm = new TestManipulatorPerDP( i );
			manipulators.put( tm.dp.getDirectory(), tm );
			directoryProviders.add( tm.dp );
		}
	}

	public static class TestManipulatorPerDP {
		private final AtomicBoolean isIndexReaderCurrent = new AtomicBoolean( false );//starts at true, see MockIndexReader contructor
		private final AtomicBoolean isReaderCreated = new AtomicBoolean( false );
		private final DirectoryProvider dp = new RAMDirectoryProvider();

		public TestManipulatorPerDP(int seed) {
			dp.initialize( "dp" + seed, new Properties(), null );
			dp.start();
		}

		public void setIndexChanged() {
			isIndexReaderCurrent.set( false );
		}

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
			throw new IllegalStateException( "IndexReader1 created twice" );
		}
		else {
			return new MockIndexReader( manipulatorPerDP.isIndexReaderCurrent );
		}
	}

	@Override
	public void initialize(Properties props, BuildContext context) {
		try {
			for ( Directory directory : manipulators.keySet() ) {
				currentReaders.put( directory, new PerDirectoryLatestReader( directory ) );
			}
		}
		catch ( IOException e ) {
			throw new SearchException( "Unable to open Lucene IndexReader", e );
		}
	}

	public boolean areAllOldReferencesGone() {
		int numReferencesReaders = allReaders.size();
		int numExpectedActiveReaders = manipulators.size();
		return numReferencesReaders == numExpectedActiveReaders;
	}

	public List<MockIndexReader> getCreatedIndexReaders() {
		return createdReadersHistory;
	}

	public MockIndexReader getCurrentMockReaderPerDP(DirectoryProvider dp) {
		IndexReader[] indexReaders = ReaderProviderHelper.getSubReadersFromMultiReader(
				( MultiReader ) super.openReader(
						new DirectoryProvider[] { dp }
				)
		);
		if ( indexReaders.length != 1 ) {
			throw new IllegalStateException( "Expecting one reader" );
		}
		return ( MockIndexReader ) indexReaders[0];
	}

	public class MockIndexReader extends IndexReader {

		private final AtomicBoolean closed = new AtomicBoolean( false );
		private final AtomicBoolean hasAlreadyBeenReOpened = new AtomicBoolean( false );
		private final AtomicBoolean isIndexReaderCurrent;

		MockIndexReader(AtomicBoolean isIndexReaderCurrent) {
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
		public Collection getFieldNames(FieldOption fldOption) {
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

//		@Override not defined in Lucene 2.9, added in 3.0
		protected void doCommit(Map<String, String> commitUserData) {
			throw new UnsupportedOperationException();
		}

//		@Override not defined in Lucene 3.0, existed before
		protected void doCommit() {
			throw new UnsupportedOperationException();
		}

	}

}

package org.hibernate.search.test.reader.functionality;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.index.TermVectorMapper;
import org.hibernate.search.reader.SharingBufferReaderProvider;
import org.hibernate.search.store.DirectoryProvider;

/**
 * @author Sanne Grinovero
 */
public class TestableSharingBufferReaderProvider extends SharingBufferReaderProvider {
	
	private final AtomicBoolean isIndexReaderCurrent = new AtomicBoolean( false );//starts at true, see MockIndexReader contructor
	private final AtomicBoolean factoryCalled = new AtomicBoolean( false );
	private final Vector<MockIndexReader> createdReadersHistory = new Vector<MockIndexReader>( 500 );
	private final MockIndexReader firstIndexReader = new MockIndexReader();
	
	@Override
	protected IndexReader readerFactory(int length, IndexReader[] readers) {
		if ( factoryCalled.compareAndSet( false, true) ) {
			return firstIndexReader;
		}
		else {
			throw new IllegalStateException( "factory for reader called more than once" );
		}
	}
	
	public void setToDirtyState() {
		isIndexReaderCurrent.set( false );
	}
	
	public boolean isMapEmpty(){
		return super.oldReaders.isEmpty();
	}
	
	public List<MockIndexReader> getCreatedIndexReaders(){
		return createdReadersHistory;
	}
	
	public MockIndexReader fakeOpenReader() {
//		System.out.println( "tracking "+oldReaders.size() + " old readers." );
		return (MockIndexReader) super.openReader( new DirectoryProvider[0] );
	}
	
	public class MockIndexReader extends IndexReader {
		
		private final AtomicBoolean closed = new AtomicBoolean( false );
		private final AtomicBoolean hasAlreadyBeenReOpened = new AtomicBoolean( false );
		
		MockIndexReader(){
			createdReadersHistory.add( this );
			if ( ! isIndexReaderCurrent.compareAndSet(false, true) ) {
				throw new IllegalStateException( "Unnecessarily reopened" );
			}
		}
		
		public final boolean isClosed() {
			return closed.get();
		}
		
		@Override
		protected void doClose() throws IOException {
			boolean okToClose = closed.compareAndSet(false, true);
			if ( ! okToClose ) {
				throw new IllegalStateException( "Attempt to close a closed IndexReader" );
			}
			if ( ! hasAlreadyBeenReOpened.get() ){
				throw new IllegalStateException( "Attempt to close the most current IndexReader" );
			}
		}
		
		@Override
		public synchronized IndexReader reopen(){
			if ( isIndexReaderCurrent.get() ) {
				return this;
			}
			else {
				if ( hasAlreadyBeenReOpened.compareAndSet( false, true) ) {
					return new MockIndexReader();
				}
				else
					throw new IllegalStateException( "Attempt to reopen an old IndexReader more than once" );
			}
		}

		@Override
		protected void doCommit() {
			throw new UnsupportedOperationException();
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
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isDeleted(int n) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int maxDoc() {
			throw new UnsupportedOperationException();
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
		
	}

}

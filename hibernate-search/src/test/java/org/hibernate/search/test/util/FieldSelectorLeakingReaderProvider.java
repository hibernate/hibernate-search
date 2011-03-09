/* 
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.search.test.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StaleReaderException;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.index.TermVectorMapper;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.hibernate.search.SearchException;
import org.hibernate.search.reader.NotSharedReaderProvider;
import org.hibernate.search.reader.ReaderProvider;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.util.ReflectionHelper;

import static junit.framework.Assert.*;

/**
 * ReaderProvider to inspect the type of FieldSelector being applied.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class FieldSelectorLeakingReaderProvider extends NotSharedReaderProvider implements ReaderProvider {
	
	private static volatile FieldSelector fieldSelector;
	
	public static void resetFieldSelector() {
		fieldSelector = null;
	}
	
	/**
	 * Verifies the FieldSelector being used contains the listed fieldnames (and no more).
	 * Note that DocumentBuilder.CLASS_FIELDNAME is always used.
	 * @param expectedFieldNames
	 */
	public static void assertFieldSelectorEnabled(String... expectedFieldNames) {
		if ( expectedFieldNames == null || expectedFieldNames.length == 0 ) {
			assertNull( FieldSelectorLeakingReaderProvider.fieldSelector );
		}
		else {
			assertNotNull( FieldSelectorLeakingReaderProvider.fieldSelector );
			MapFieldSelector selector = (MapFieldSelector) fieldSelector;
			Map<String, FieldSelectorResult> fieldSelections;
			try {
				Field field = MapFieldSelector.class.getDeclaredField( "fieldSelections" );
				ReflectionHelper.setAccessible( field );
				fieldSelections = (Map<String, FieldSelectorResult>) field.get( selector );
			}
			catch (NoSuchFieldException e) {
				throw new SearchException( "Incompatible version of Lucene: MapFieldSelector.fieldSelections not available", e );
			}
			catch (IllegalArgumentException e) {
				throw new SearchException( "Incompatible version of Lucene: MapFieldSelector.fieldSelections not available", e );
			}
			catch (IllegalAccessException e) {
				throw new SearchException( "Incompatible version of Lucene: MapFieldSelector.fieldSelections not available", e );
			}
			assertNotNull( fieldSelections );
			assertEquals( expectedFieldNames.length, fieldSelections.size() );
		}
	}
	
	public static void assertFieldSelectorDisabled() {
		assertNull( FieldSelectorLeakingReaderProvider.fieldSelector );
	}
	
	@Override
	public IndexReader openReader(DirectoryProvider... directoryProviders) {
		IndexReader indexReader = super.openReader( directoryProviders );
		IndexReader leakingReader = new LeakingIndexReader( indexReader );
		return leakingReader;
	}
	
	/**
	 * Delegates are created via IDE code generation with some changes:
	 * - the method {@link #document(int, FieldSelector)}
	 * - method {@link #document(int)}
	 * - the last ones: can't delegate as the method is not visible
	 */
	class LeakingIndexReader extends IndexReader {
		
		private final IndexReader delegate;

		LeakingIndexReader(IndexReader wrapped) {
			this.delegate = wrapped;
		}

		public int hashCode() {
			return delegate.hashCode();
		}

		public boolean equals(Object obj) {
			return delegate.equals( obj );
		}

		public int getRefCount() {
			return delegate.getRefCount();
		}

		public void incRef() {
			delegate.incRef();
		}

		public void decRef() throws IOException {
			delegate.decRef();
		}

		public String toString() {
			return delegate.toString();
		}

		public IndexReader reopen() throws CorruptIndexException, IOException {
			return delegate.reopen();
		}

		public IndexReader reopen(boolean openReadOnly) throws CorruptIndexException, IOException {
			return delegate.reopen( openReadOnly );
		}

		public IndexReader reopen(IndexCommit commit) throws CorruptIndexException, IOException {
			return delegate.reopen( commit );
		}

		public Object clone() {
			return delegate.clone();
		}

		public IndexReader clone(boolean openReadOnly) throws CorruptIndexException, IOException {
			return delegate.clone( openReadOnly );
		}

		public Directory directory() {
			return delegate.directory();
		}

		public long getVersion() {
			return delegate.getVersion();
		}

		public Map<String, String> getCommitUserData() {
			return delegate.getCommitUserData();
		}

		public boolean isCurrent() throws CorruptIndexException, IOException {
			return delegate.isCurrent();
		}

		public boolean isOptimized() {
			return delegate.isOptimized();
		}

		public TermFreqVector[] getTermFreqVectors(int docNumber) throws IOException {
			return delegate.getTermFreqVectors( docNumber );
		}

		public TermFreqVector getTermFreqVector(int docNumber, String field) throws IOException {
			return delegate.getTermFreqVector( docNumber, field );
		}

		public void getTermFreqVector(int docNumber, String field, TermVectorMapper mapper) throws IOException {
			delegate.getTermFreqVector( docNumber, field, mapper );
		}

		public void getTermFreqVector(int docNumber, TermVectorMapper mapper) throws IOException {
			delegate.getTermFreqVector( docNumber, mapper );
		}

		public int numDocs() {
			return delegate.numDocs();
		}

		public int maxDoc() {
			return delegate.maxDoc();
		}

		public int numDeletedDocs() {
			return delegate.numDeletedDocs();
		}

		public Document document(int n) throws CorruptIndexException, IOException {
			FieldSelectorLeakingReaderProvider.fieldSelector = null;
			return delegate.document( n );
		}

		public Document document(int n, FieldSelector fieldSelector) throws CorruptIndexException, IOException {
			FieldSelectorLeakingReaderProvider.fieldSelector = fieldSelector;
			return delegate.document( n, fieldSelector );
		}

		public boolean isDeleted(int n) {
			return delegate.isDeleted( n );
		}

		public boolean hasDeletions() {
			return delegate.hasDeletions();
		}

		public boolean hasNorms(String field) throws IOException {
			return delegate.hasNorms( field );
		}

		public byte[] norms(String field) throws IOException {
			return delegate.norms( field );
		}

		public void norms(String field, byte[] bytes, int offset) throws IOException {
			delegate.norms( field, bytes, offset );
		}

		public void setNorm(int doc, String field, byte value) throws StaleReaderException, CorruptIndexException, LockObtainFailedException, IOException {
			delegate.setNorm( doc, field, value );
		}

		public void setNorm(int doc, String field, float value) throws StaleReaderException, CorruptIndexException, LockObtainFailedException, IOException {
			delegate.setNorm( doc, field, value );
		}

		public TermEnum terms() throws IOException {
			return delegate.terms();
		}

		public TermEnum terms(Term t) throws IOException {
			return delegate.terms( t );
		}

		public int docFreq(Term t) throws IOException {
			return delegate.docFreq( t );
		}

		public TermDocs termDocs(Term term) throws IOException {
			return delegate.termDocs( term );
		}

		public TermDocs termDocs() throws IOException {
			return delegate.termDocs();
		}

		public TermPositions termPositions(Term term) throws IOException {
			return delegate.termPositions( term );
		}

		public TermPositions termPositions() throws IOException {
			return delegate.termPositions();
		}

		public void deleteDocument(int docNum) throws StaleReaderException, CorruptIndexException, LockObtainFailedException, IOException {
			delegate.deleteDocument( docNum );
		}

		public int deleteDocuments(Term term) throws StaleReaderException, CorruptIndexException, LockObtainFailedException, IOException {
			return delegate.deleteDocuments( term );
		}

		public void undeleteAll() throws StaleReaderException, CorruptIndexException, LockObtainFailedException, IOException {
			delegate.undeleteAll();
		}

		public Collection<String> getFieldNames(FieldOption fldOption) {
			return delegate.getFieldNames( fldOption );
		}

		public IndexCommit getIndexCommit() throws IOException {
			return delegate.getIndexCommit();
		}

		public IndexReader[] getSequentialSubReaders() {
			return delegate.getSequentialSubReaders();
		}

		public Object getCoreCacheKey() {
			return delegate.getCoreCacheKey();
		}
		
		public Object getDeletesCacheKey() {
			return delegate.getDeletesCacheKey();
		}

		public long getUniqueTermCount() throws IOException {
			return delegate.getUniqueTermCount();
		}

		public int getTermInfosIndexDivisor() {
			return delegate.getTermInfosIndexDivisor();
		}

		protected void doSetNorm(int doc, String field, byte value) throws CorruptIndexException, IOException {
			throw new UnsupportedOperationException("delegate method is not visible - hope we don't need it");
		}

		protected void doDelete(int docNum) throws CorruptIndexException, IOException {
			throw new UnsupportedOperationException("delegate method is not visible - hope we don't need it");
		}

		protected void doUndeleteAll() throws CorruptIndexException, IOException {
			throw new UnsupportedOperationException("delegate method is not visible - hope we don't need it");
		}

		protected void doCommit(Map<String, String> commitUserData) throws IOException {
			throw new UnsupportedOperationException("delegate method is not visible - hope we don't need it");
		}

		protected void doClose() throws IOException {
			throw new UnsupportedOperationException("delegate method is not visible - hope we don't need it");
		}
		
	}

}

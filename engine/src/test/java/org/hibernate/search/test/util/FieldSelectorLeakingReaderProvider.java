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
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.index.TermVectorMapper;
import org.apache.lucene.store.Directory;
import org.hibernate.search.SearchException;
import org.hibernate.search.indexes.impl.NotSharedReaderProvider;
import org.hibernate.search.indexes.spi.ReaderProvider;
import org.hibernate.search.util.impl.ReflectionHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

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
			for ( String field : expectedFieldNames ) {
				assertTrue( "did not contain field: " + field, fieldSelections.containsKey( field ) );
				assertTrue( FieldSelectorResult.LOAD.equals( fieldSelections.get( field ) ) ||
						FieldSelectorResult.LOAD_AND_BREAK.equals( fieldSelections.get( field ) ) );
			}
		}
	}

	public static void assertFieldSelectorDisabled() {
		assertNull( FieldSelectorLeakingReaderProvider.fieldSelector );
	}

	@Override
	public IndexReader openIndexReader() {
		IndexReader indexReader = super.openIndexReader();
		IndexReader leakingReader = new LeakingIndexReader( indexReader );
		return leakingReader;
	}

	/**
	 * Delegates are created via IDE code generation with some changes:
	 * - the method {@link #document(int, FieldSelector)}
	 * - method {@link #document(int)}
	 * - the last ones: can't delegate as the method is not visible
	 */
	private static class LeakingIndexReader extends IndexReader {

		private final IndexReader delegate;

		LeakingIndexReader(IndexReader wrapped) {
			this.delegate = wrapped;
		}

		@Override
		public int hashCode() {
			return delegate.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return delegate.equals( obj );
		}

		@Override
		public String toString() {
			return delegate.toString();
		}

		@Override
		public Object clone() {
			return delegate.clone();
		}

		@Override
		public IndexReader clone(boolean openReadOnly) throws IOException {
			return delegate.clone( openReadOnly );
		}

		@Override
		public Directory directory() {
			return delegate.directory();
		}

		@Override
		public long getVersion() {
			return delegate.getVersion();
		}

		@Override
		public Map<String, String> getCommitUserData() {
			return delegate.getCommitUserData();
		}

		@Override
		public boolean isCurrent() throws IOException {
			return delegate.isCurrent();
		}

		@Override
		public TermFreqVector[] getTermFreqVectors(int docNumber) throws IOException {
			return delegate.getTermFreqVectors( docNumber );
		}

		@Override
		public TermFreqVector getTermFreqVector(int docNumber, String field) throws IOException {
			return delegate.getTermFreqVector( docNumber, field );
		}

		@Override
		public void getTermFreqVector(int docNumber, String field, TermVectorMapper mapper) throws IOException {
			delegate.getTermFreqVector( docNumber, field, mapper );
		}

		@Override
		public void getTermFreqVector(int docNumber, TermVectorMapper mapper) throws IOException {
			delegate.getTermFreqVector( docNumber, mapper );
		}

		@Override
		public int numDocs() {
			return delegate.numDocs();
		}

		@Override
		public int maxDoc() {
			return delegate.maxDoc();
		}

		@Override
		public Document document(int n, FieldSelector fieldSelector) throws IOException {
			FieldSelectorLeakingReaderProvider.fieldSelector = fieldSelector;
			return delegate.document( n, fieldSelector );
		}

		@Override
		public boolean isDeleted(int n) {
			return delegate.isDeleted( n );
		}

		@Override
		public boolean hasDeletions() {
			return delegate.hasDeletions();
		}

		@Override
		public boolean hasNorms(String field) throws IOException {
			return delegate.hasNorms( field );
		}

		@Override
		public byte[] norms(String field) throws IOException {
			return delegate.norms( field );
		}

		@Override
		public void norms(String field, byte[] bytes, int offset) throws IOException {
			delegate.norms( field, bytes, offset );
		}

		@Override
		public TermEnum terms() throws IOException {
			return delegate.terms();
		}

		@Override
		public TermEnum terms(Term t) throws IOException {
			return delegate.terms( t );
		}

		@Override
		public int docFreq(Term t) throws IOException {
			return delegate.docFreq( t );
		}

		@Override
		public TermDocs termDocs(Term term) throws IOException {
			return delegate.termDocs( term );
		}

		@Override
		public TermDocs termDocs() throws IOException {
			return delegate.termDocs();
		}

		@Override
		public TermPositions termPositions() throws IOException {
			return delegate.termPositions();
		}

		@Override
		public IndexCommit getIndexCommit() throws IOException {
			return delegate.getIndexCommit();
		}

		@Override
		public IndexReader[] getSequentialSubReaders() {
			return delegate.getSequentialSubReaders();
		}

		@Override
		public Object getCoreCacheKey() {
			return delegate.getCoreCacheKey();
		}

		@Override
		public Object getDeletesCacheKey() {
			return delegate.getDeletesCacheKey();
		}

		@Override
		public long getUniqueTermCount() throws IOException {
			return delegate.getUniqueTermCount();
		}

		@Override
		public int getTermInfosIndexDivisor() {
			return delegate.getTermInfosIndexDivisor();
		}

		@Override
		protected void doSetNorm(int doc, String field, byte value) throws IOException {
			throw new UnsupportedOperationException("delegate method is not visible - hope we don't need it");
		}

		@Override
		protected void doDelete(int docNum) throws IOException {
			throw new UnsupportedOperationException("delegate method is not visible - hope we don't need it");
		}

		@Override
		protected void doUndeleteAll() throws IOException {
			throw new UnsupportedOperationException("delegate method is not visible - hope we don't need it");
		}

		@Override
		protected void doCommit(Map<String, String> commitUserData) throws IOException {
			//can't implement as method is not visibile
			//not important either as method is deprecated and all Readers are read-only: nothing to commit
			//also, method is going to be removed.
		}

		@Override
		protected void doClose() throws IOException {
			//can't implement as method is not visibile
			//not important either as method is deprecated and all Readers are read-only: nothing to flush on close
			//also, method is going to be removed.
		}

		@Override
		public FieldInfos getFieldInfos() {
			return delegate.getFieldInfos();
		}

	}

}

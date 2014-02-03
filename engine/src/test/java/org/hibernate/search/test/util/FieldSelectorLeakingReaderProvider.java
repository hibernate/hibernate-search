/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.test.util;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfo.DocValuesType;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.FilterAtomicReader;
import org.apache.lucene.index.FilterDirectoryReader;
import org.apache.lucene.index.FilterDirectoryReader.SubReaderWrapper;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.index.StoredFieldVisitor.Status;
import org.hibernate.search.indexes.impl.NotSharedReaderProvider;
import org.hibernate.search.indexes.spi.ReaderProvider;
import org.hibernate.search.query.engine.impl.ReusableDocumentStoredFieldVisitor;

/**
 * ReaderProvider to inspect the type of FieldSelector being applied.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public final class FieldSelectorLeakingReaderProvider extends NotSharedReaderProvider implements ReaderProvider {

	private static volatile StoredFieldVisitor fieldSelector;

	public static void resetFieldSelector() {
		fieldSelector = null;
	}

	/**
	 * Verifies the FieldSelector being used contains the listed fieldnames (and no more).
	 * Note that DocumentBuilder.CLASS_FIELDNAME is always used.
	 * @param expectedFieldNames
	 * @throws IOException
	 */
	public static void assertFieldSelectorEnabled(String... expectedFieldNames) throws IOException {
		if ( expectedFieldNames == null || expectedFieldNames.length == 0 ) {
			assertFieldSelectorDisabled();
		}
		else {
			assertNotNull( FieldSelectorLeakingReaderProvider.fieldSelector );
			assertTrue( fieldSelector instanceof ReusableDocumentStoredFieldVisitor );
			ReusableDocumentStoredFieldVisitor visitor = (ReusableDocumentStoredFieldVisitor) fieldSelector;
			assertEquals( expectedFieldNames.length, visitor.countAcceptedFields() );
			for ( String fieldName : expectedFieldNames ) {
				FieldInfo fieldId = forgeFieldInfo( fieldName );
				assertEquals( Status.YES, visitor.needsField( fieldId ) );
			}
		}
	}

	private static FieldInfo forgeFieldInfo(String fieldName) {
		//Specific options besides the field name aren't important:
		//we just need to satisfy the constructor
		return new FieldInfo( fieldName, true, 0, false, true, false,
				IndexOptions.DOCS_ONLY, DocValuesType.SORTED, DocValuesType.SORTED,
				null );
	}

	public static void assertFieldSelectorDisabled() {
		StoredFieldVisitor fieldVisitor = FieldSelectorLeakingReaderProvider.fieldSelector;
		//DocumentStoredFieldVisitor is the type of the default fieldVisitor, so it's expected:
		assertTrue( fieldVisitor == null || fieldVisitor instanceof DocumentStoredFieldVisitor );
	}

	public DirectoryReader openIndexReader() {
		DirectoryReader originalReader = super.openIndexReader();
		LeakingDirectoryReader wrappedReader = new LeakingDirectoryReader( originalReader );
		return wrappedReader;
	}

	private final class LeakingDirectoryReader extends FilterDirectoryReader {
		public LeakingDirectoryReader(DirectoryReader in) {
			super( in, new LeakingSubReaderWrapper() );
		}
		@Override
		protected DirectoryReader doWrapDirectoryReader(DirectoryReader in) {
			return new LeakingDirectoryReader( in );
		}
	}

	private final class LeakingSubReaderWrapper extends SubReaderWrapper {
		@Override
		public AtomicReader wrap(AtomicReader reader) {
			return new LeakingAtomicReader( reader );
		}
	}

	private final class LeakingAtomicReader extends FilterAtomicReader {
		public LeakingAtomicReader(AtomicReader in) {
			super( in );
		}
		@Override
		public void document(int docID, StoredFieldVisitor visitor) throws IOException {
			super.document( docID, visitor );
			FieldSelectorLeakingReaderProvider.fieldSelector = visitor;
		}
	}

}

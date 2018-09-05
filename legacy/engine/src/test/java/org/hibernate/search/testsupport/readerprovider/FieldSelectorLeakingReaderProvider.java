/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.testsupport.readerprovider;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;

import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FilterDirectoryReader;
import org.apache.lucene.index.FilterDirectoryReader.SubReaderWrapper;
import org.apache.lucene.index.FilterLeafReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.index.StoredFieldVisitor.Status;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.impl.NotSharedReaderProvider;
import org.hibernate.search.indexes.spi.ReaderProvider;
import org.hibernate.search.query.engine.impl.ReusableDocumentStoredFieldVisitor;

/**
 * {@code ReaderProvider} to inspect the type of {@code FieldSelector} being applied.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public final class FieldSelectorLeakingReaderProvider extends NotSharedReaderProvider implements ReaderProvider {

	private static volatile StoredFieldVisitor fieldSelector;

	public static void resetFieldSelector() {
		fieldSelector = null;
	}

	/**
	 * Verifies the FieldSelector being used contains the listed field names (and no more).
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

	static FieldInfo forgeFieldInfo(String fieldName) {
		//Specific options besides the field name aren't important:
		//we just need to satisfy the constructor
		return new FieldInfo( fieldName, 0, false, true, false,
				IndexOptions.DOCS, DocValuesType.SORTED,
				1l, Collections.<String,String>emptyMap() );
	}

	public static void assertFieldSelectorDisabled() {
		StoredFieldVisitor fieldVisitor = FieldSelectorLeakingReaderProvider.fieldSelector;
		//DocumentStoredFieldVisitor is the type of the default fieldVisitor, so it's expected:
		assertTrue( fieldVisitor == null || fieldVisitor instanceof DocumentStoredFieldVisitor );
	}

	@Override
	public DirectoryReader openIndexReader() {
		DirectoryReader originalReader = super.openIndexReader();
		try {
			LeakingDirectoryReader wrappedReader = new LeakingDirectoryReader( originalReader );
			return wrappedReader;
		}
		catch (IOException ioe) {
			throw new SearchException( ioe );
		}
	}

	private final class LeakingDirectoryReader extends FilterDirectoryReader {
		public LeakingDirectoryReader(DirectoryReader in) throws IOException {
			super( in, new LeakingSubReaderWrapper() );
		}
		@Override
		protected DirectoryReader doWrapDirectoryReader(DirectoryReader in) throws IOException {
			return new LeakingDirectoryReader( in );
		}
	}

	private final class LeakingSubReaderWrapper extends SubReaderWrapper {
		@Override
		public LeafReader wrap(LeafReader reader) {
			return new LeakingAtomicReader( reader );
		}
	}

	private final class LeakingAtomicReader extends FilterLeafReader {
		public LeakingAtomicReader(LeafReader in) {
			super( in );
		}
		@Override
		public void document(int docID, StoredFieldVisitor visitor) throws IOException {
			super.document( docID, visitor );
			FieldSelectorLeakingReaderProvider.fieldSelector = visitor;
		}
	}

}

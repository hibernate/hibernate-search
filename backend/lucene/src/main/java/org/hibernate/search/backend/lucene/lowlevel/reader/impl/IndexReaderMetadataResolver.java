/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.reader.impl;

import java.util.Map;

import org.hibernate.search.util.common.AssertionFailure;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;

public final class IndexReaderMetadataResolver {

	private final Map<DirectoryReader, String> mappedTypeNameByDirectoryReader;

	IndexReaderMetadataResolver(Map<DirectoryReader, String> mappedTypeNameByDirectoryReader) {
		this.mappedTypeNameByDirectoryReader = mappedTypeNameByDirectoryReader;
	}

	public String resolveMappedTypeName(LeafReaderContext context) {
		return mappedTypeNameByDirectoryReader.get( getDirectoryReader( context ) );
	}

	private DirectoryReader getDirectoryReader(LeafReaderContext context) {
		IndexReaderContext current = context;
		while ( current != null && !( current.reader() instanceof DirectoryReader ) ) {
			current = current.parent;
		}
		if ( current == null ) {
			throw new AssertionFailure(
					"Unexpectedly got a reader context that has no DirectoryReader parent: " + context
			);
		}

		return (DirectoryReader) current.reader();
	}
}

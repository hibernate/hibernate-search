/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.performance.backend.base.testsupport.index;

import java.util.List;

import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.infra.ThreadParams;

/**
 * Partitions indexes so that each thread targets a single set of documents.
 */
@CompilerControl(CompilerControl.Mode.INLINE)
public class PerThreadIndexPartition {

	private final MappedIndex indexTargetedByThread;

	private final long documentIdOffset;
	private final long documentIdGap;

	public PerThreadIndexPartition(AbstractBackendHolder indexHolder, IndexInitializer indexInitializer,
			ThreadParams threadParams) {
		int threadIndex = threadParams.getThreadIndex();
		List<MappedIndex> indexes = indexHolder.getIndexes();
		indexTargetedByThread = indexes.get( threadIndex % indexes.size() );

		documentIdOffset =
				// Avoid conflict between initial documents and new documents:
				indexInitializer.getInitialIndexSize()
				// Avoid conflict between threads: use a different starting point based on the thread index...
				+ threadIndex;
		// ... and then a gap based on the thread count
		documentIdGap = threadParams.getThreadCount();
	}

	public MappedIndex getIndex() {
		return indexTargetedByThread;
	}

	public long toDocumentId(long idInThread) {
		// Avoid conflict between threads by using a different starting point based on the thread index
		return documentIdOffset + idInThread * documentIdGap;
	}

}

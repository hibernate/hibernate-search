/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.backend;

import java.io.IOException;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;

public class LuceneBackendTestHelpers {

	private LuceneBackendTestHelpers() {
		//Utility class: not to be instantiated
	}

	public static boolean isLocked(Directory directory) throws IOException {
		try {
			directory.obtainLock( org.apache.lucene.index.IndexWriter.WRITE_LOCK_NAME ).close();
			return false;
		}
		catch (LockObtainFailedException failed) {
			return true;
		}
	}

}

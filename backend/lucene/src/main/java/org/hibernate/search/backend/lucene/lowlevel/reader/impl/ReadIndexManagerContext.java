/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.reader.impl;

import java.io.IOException;
import java.util.Set;

/**
 * An interface with knowledge of the index manager internals,
 * able to retrieve components related to index reading.
 */
public interface ReadIndexManagerContext {

	void openIndexReaders(Set<String> routingKeys, DirectoryReaderCollector readerCollector) throws IOException;

}

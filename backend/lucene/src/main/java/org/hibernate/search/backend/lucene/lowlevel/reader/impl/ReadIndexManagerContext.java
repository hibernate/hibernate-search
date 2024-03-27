/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

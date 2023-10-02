/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.common.impl;

import org.hibernate.search.backend.lucene.lowlevel.reader.impl.ReadIndexManagerContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexIdentifierContext;

public interface LuceneSearchIndexContext extends ReadIndexManagerContext {

	SearchIndexIdentifierContext identifier();

}

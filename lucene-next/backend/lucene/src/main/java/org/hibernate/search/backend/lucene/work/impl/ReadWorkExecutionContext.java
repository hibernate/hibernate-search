/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.work.impl;

import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.search.IndexSearcher;

public interface ReadWorkExecutionContext {

	IndexSearcher createSearcher();

	IndexReaderMetadataResolver getIndexReaderMetadataResolver();

	EventContext getEventContext();

}

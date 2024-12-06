/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;
import org.hibernate.search.util.common.reporting.EventContext;

public interface IndexingWorkExecutionContext {

	EventContext getEventContext();

	IndexWriterDelegator getIndexWriterDelegator() throws IOException;

}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.work.impl;

import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessor;
import org.hibernate.search.util.common.reporting.EventContext;

public interface IndexManagementWorkExecutionContext {

	EventContext getEventContext();

	IndexAccessor getIndexAccessor();

}

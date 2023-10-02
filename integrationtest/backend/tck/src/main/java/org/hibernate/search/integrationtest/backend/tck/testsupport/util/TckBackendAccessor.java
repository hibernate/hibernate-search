/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import java.io.Closeable;

public interface TckBackendAccessor extends Closeable {

	void ensureIndexingOperationsFail(String indexName);

	void ensureFlushMergeRefreshOperationsFail(String indexName);

}

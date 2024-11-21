/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.work.impl;


public interface SingleDocumentIndexingWork extends IndexingWork<Long> {

	String getEntityTypeName();

	Object getEntityIdentifier();

}

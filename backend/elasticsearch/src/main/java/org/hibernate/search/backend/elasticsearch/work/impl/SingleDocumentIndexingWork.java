/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

public interface SingleDocumentIndexingWork extends IndexingWork<Void> {

	String getEntityTypeName();

	Object getEntityIdentifier();

}

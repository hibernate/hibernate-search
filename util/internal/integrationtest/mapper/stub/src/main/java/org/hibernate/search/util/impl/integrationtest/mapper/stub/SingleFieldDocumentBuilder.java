/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

public interface SingleFieldDocumentBuilder<T> {

	void emptyDocument(String documentId);

	void document(String documentId, T fieldValue);

}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.DocumentReferenceProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;

public final class DocumentReferenceProjectionOptionsStepImpl
		implements DocumentReferenceProjectionOptionsStep<DocumentReferenceProjectionOptionsStepImpl> {

	private final SearchProjection<DocumentReference> documentReferenceProjection;

	public DocumentReferenceProjectionOptionsStepImpl(SearchProjectionDslContext<?> dslContext) {
		this.documentReferenceProjection = dslContext.scope().projectionBuilders().documentReference();
	}

	@Override
	public SearchProjection<DocumentReference> toProjection() {
		return documentReferenceProjection;
	}

}

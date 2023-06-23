/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

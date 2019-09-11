/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.DocumentReferenceProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;


public class DocumentReferenceProjectionOptionsStepImpl implements DocumentReferenceProjectionOptionsStep {

	private final DocumentReferenceProjectionBuilder documentReferenceProjectionBuilder;

	DocumentReferenceProjectionOptionsStepImpl(SearchProjectionBuilderFactory factory) {
		this.documentReferenceProjectionBuilder = factory.documentReference();
	}

	@Override
	public SearchProjection<DocumentReference> toProjection() {
		return documentReferenceProjectionBuilder.build();
	}

}

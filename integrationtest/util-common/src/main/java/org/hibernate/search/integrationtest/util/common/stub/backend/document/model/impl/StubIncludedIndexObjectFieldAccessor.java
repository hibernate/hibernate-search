/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.integrationtest.util.common.stub.backend.document.impl.StubDocumentElement;

class StubIncludedIndexObjectFieldAccessor implements IndexObjectFieldAccessor {

	private final String relativeName;

	StubIncludedIndexObjectFieldAccessor(String relativeName) {
		this.relativeName = relativeName;
	}

	@Override
	public DocumentElement add(DocumentElement target) {
		StubDocumentElement stubTarget = (StubDocumentElement) target;
		return stubTarget.putChild( relativeName );
	}

	@Override
	public void addMissing(DocumentElement target) {
		StubDocumentElement stubTarget = (StubDocumentElement) target;
		stubTarget.putMissingChild( relativeName );
	}
}

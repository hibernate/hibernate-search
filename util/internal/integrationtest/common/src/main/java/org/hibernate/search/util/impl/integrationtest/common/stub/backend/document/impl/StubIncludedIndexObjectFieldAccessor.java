/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;

public class StubIncludedIndexObjectFieldAccessor implements IndexObjectFieldAccessor {

	private final String absolutePath;
	private final String relativeFieldName;

	public StubIncludedIndexObjectFieldAccessor(String absolutePath, String relativeFieldName) {
		this.absolutePath = absolutePath;
		this.relativeFieldName = relativeFieldName;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + absolutePath + "]";
	}

	@Override
	public DocumentElement add(DocumentElement target) {
		StubDocumentElement stubTarget = (StubDocumentElement) target;
		return stubTarget.putChild( relativeFieldName );
	}

	@Override
	public void addMissing(DocumentElement target) {
		StubDocumentElement stubTarget = (StubDocumentElement) target;
		stubTarget.putMissingChild( relativeFieldName );
	}
}

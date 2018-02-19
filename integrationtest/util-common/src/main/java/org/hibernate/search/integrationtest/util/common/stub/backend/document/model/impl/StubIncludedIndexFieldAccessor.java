/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.integrationtest.util.common.stub.backend.document.impl.StubDocumentElement;

class StubIncludedIndexFieldAccessor<T> implements IndexFieldAccessor<T> {
	private final String relativeName;

	StubIncludedIndexFieldAccessor(String relativeName) {
		this.relativeName = relativeName;
	}

	@Override
	public void write(DocumentElement target, T value) {
		StubDocumentElement stubTarget = (StubDocumentElement) target;
		stubTarget.putValue( relativeName, value );
	}
}

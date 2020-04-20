/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.spi;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;

public class NoOpDocumentElement implements DocumentElement {

	static final NoOpDocumentElement INSTANCE = new NoOpDocumentElement();

	public static NoOpDocumentElement get() {
		return INSTANCE;
	}

	private NoOpDocumentElement() {
	}

	@Override
	public <F> void addValue(IndexFieldReference<F> fieldReference, F value) {
		// No-op
	}

	@Override
	public DocumentElement addObject(IndexObjectFieldReference fieldReference) {
		// No-op; just return a no-op child
		return INSTANCE;
	}

	@Override
	public void addNullObject(IndexObjectFieldReference fieldReference) {
		// No-op
	}

	@Override
	public void addValue(String relativeFieldName, Object value) {
		// No-op
	}

	@Override
	public DocumentElement addObject(String relativeFieldName) {
		// No-op; just return a no-op child
		return INSTANCE;
	}

	@Override
	public void addNullObject(String relativeFieldName) {
		// No-op
	}
}

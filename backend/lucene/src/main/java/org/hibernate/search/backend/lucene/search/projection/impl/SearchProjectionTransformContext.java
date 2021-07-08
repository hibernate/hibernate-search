/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentIdentifierValueConvertContextImpl;

public class SearchProjectionTransformContext {

	private final FromDocumentValueConvertContext fromDocumentValueConvertContext;
	private final FromDocumentIdentifierValueConvertContextImpl fromDocumentIdentifierValueConvertContext;

	private boolean hasFailedLoad = false;

	public SearchProjectionTransformContext(FromDocumentValueConvertContext fromDocumentValueConvertContext,
			FromDocumentIdentifierValueConvertContextImpl identifierConvertContext) {
		this.fromDocumentValueConvertContext = fromDocumentValueConvertContext;
		this.fromDocumentIdentifierValueConvertContext = identifierConvertContext;
	}

	FromDocumentValueConvertContext fromDocumentValueConvertContext() {
		return fromDocumentValueConvertContext;
	}

	FromDocumentIdentifierValueConvertContext fromDocumentIdentifierValueConvertContext() {
		return fromDocumentIdentifierValueConvertContext;
	}

	void reportFailedLoad() {
		hasFailedLoad = true;
	}

	public boolean hasFailedLoad() {
		return hasFailedLoad;
	}

	public void reset() {
		hasFailedLoad = false;
	}
}

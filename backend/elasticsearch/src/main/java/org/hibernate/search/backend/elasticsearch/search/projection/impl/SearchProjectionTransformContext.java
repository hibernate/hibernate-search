/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentIdentifierValueConvertContext;

public class SearchProjectionTransformContext {

	private final FromDocumentFieldValueConvertContext fromDocumentFieldValueConvertContext;
	private final FromDocumentIdentifierValueConvertContext fromDocumentIdentifierValueConvertContext;

	private boolean hasFailedLoad = false;

	public SearchProjectionTransformContext(FromDocumentFieldValueConvertContext fieldConverterContext,
			FromDocumentIdentifierValueConvertContext identifierConvertContext) {
		this.fromDocumentFieldValueConvertContext = fieldConverterContext;
		this.fromDocumentIdentifierValueConvertContext = identifierConvertContext;
	}

	FromDocumentFieldValueConvertContext fromDocumentFieldValueConvertContext() {
		return fromDocumentFieldValueConvertContext;
	}

	public FromDocumentIdentifierValueConvertContext fromDocumentIdentifierValueConvertContext() {
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

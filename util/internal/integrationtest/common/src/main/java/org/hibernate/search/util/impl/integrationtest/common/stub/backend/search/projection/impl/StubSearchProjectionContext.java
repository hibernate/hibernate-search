/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentIdentifierValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentValueConvertContextImpl;

public class StubSearchProjectionContext {

	private final FromDocumentValueConvertContext fromDocumentValueConvertContext;
	private final FromDocumentIdentifierValueConvertContextImpl fromDocumentIdentifierValueConvertContext;

	private boolean hasFailedLoad = false;

	public StubSearchProjectionContext(BackendSessionContext sessionContext) {
		fromDocumentValueConvertContext = new FromDocumentValueConvertContextImpl( sessionContext );
		fromDocumentIdentifierValueConvertContext = new FromDocumentIdentifierValueConvertContextImpl( sessionContext );
	}

	FromDocumentValueConvertContext fromDocumentValueConvertContext() {
		return fromDocumentValueConvertContext;
	}

	FromDocumentIdentifierValueConvertContextImpl fromDocumentIdentifierValueConvertContext() {
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

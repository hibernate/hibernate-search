/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentFieldValueConvertContextImpl;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;

public class StubSearchProjectionContext {

	private final FromDocumentFieldValueConvertContext fromDocumentFieldValueConvertContext;

	private boolean hasFailedLoad = false;

	public StubSearchProjectionContext(BackendSessionContext sessionContext) {
		this.fromDocumentFieldValueConvertContext = new FromDocumentFieldValueConvertContextImpl( sessionContext );
	}

	FromDocumentFieldValueConvertContext getFromDocumentFieldValueConvertContext() {
		return fromDocumentFieldValueConvertContext;
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

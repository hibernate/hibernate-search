/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentValueConvertContextImpl;

public class StubSearchProjectionContext {

	private final FromDocumentValueConvertContext fromDocumentValueConvertContext;

	private boolean hasFailedLoad = false;

	public StubSearchProjectionContext(BackendSessionContext sessionContext) {
		fromDocumentValueConvertContext = new FromDocumentValueConvertContextImpl( sessionContext );
	}

	FromDocumentValueConvertContext fromDocumentValueConvertContext() {
		return fromDocumentValueConvertContext;
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

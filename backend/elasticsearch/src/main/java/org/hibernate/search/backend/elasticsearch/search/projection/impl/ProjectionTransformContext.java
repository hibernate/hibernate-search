/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;

public class ProjectionTransformContext {

	private final FromDocumentValueConvertContext fromDocumentValueConvertContext;

	private boolean hasFailedLoad = false;

	public ProjectionTransformContext(FromDocumentValueConvertContext fromDocumentValueConvertContext) {
		this.fromDocumentValueConvertContext = fromDocumentValueConvertContext;
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

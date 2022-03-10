/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

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

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl;

import org.hibernate.search.engine.backend.document.IndexFieldReference;

public class StubIndexFieldReference<F> implements IndexFieldReference<F> {

	private final String absolutePath;
	private final String relativeFieldName;
	private final boolean enabled;

	public StubIndexFieldReference(String absolutePath, String relativeFieldName, boolean enabled) {
		this.absolutePath = absolutePath;
		this.relativeFieldName = relativeFieldName;
		this.enabled = enabled;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + absolutePath + "]";
	}

	boolean isEnabled() {
		return enabled;
	}

	String getAbsolutePath() {
		return absolutePath;
	}

	String getRelativeFieldName() {
		return relativeFieldName;
	}
}

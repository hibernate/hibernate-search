/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;

public class StubIndexObjectFieldReference implements IndexObjectFieldReference {

	private final String absolutePath;
	private final String relativeFieldName;
	private final TreeNodeInclusion inclusion;

	public StubIndexObjectFieldReference(String absolutePath, String relativeFieldName, TreeNodeInclusion inclusion) {
		this.absolutePath = absolutePath;
		this.relativeFieldName = relativeFieldName;
		this.inclusion = inclusion;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + absolutePath + "]";
	}

	public TreeNodeInclusion getInclusion() {
		return inclusion;
	}

	String getAbsolutePath() {
		return absolutePath;
	}

	String getRelativeFieldName() {
		return relativeFieldName;
	}
}

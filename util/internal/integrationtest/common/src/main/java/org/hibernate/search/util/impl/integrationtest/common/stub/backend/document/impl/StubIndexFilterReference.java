/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl;

import org.hibernate.search.engine.backend.document.IndexFilterReference;
import org.hibernate.search.engine.search.predicate.factories.FilterFactory;

public class StubIndexFilterReference<F extends FilterFactory> implements IndexFilterReference<F> {
	private final String relativeFieldName;

	private final F factory;
	private final String absolutePath;

	public StubIndexFilterReference(String absolutePath, String relativeFieldName, F factory) {
		this.relativeFieldName = relativeFieldName;
		this.factory = factory;
		this.absolutePath = absolutePath;
	}

	@Override
	public String getName() {
		return absolutePath;
	}

	@Override
	public F getFactory() {
		return factory;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + absolutePath + "]";
	}
}

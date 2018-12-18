/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.util.impl.common.LoggerFactory;
import org.hibernate.search.util.impl.common.logging.Log;

public class StubExcludedIndexFieldAccessor<F> implements IndexFieldAccessor<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String absolutePath;
	private final String relativeFieldName;

	public StubExcludedIndexFieldAccessor(String absolutePath, String relativeFieldName) {
		this.absolutePath = absolutePath;
		this.relativeFieldName = relativeFieldName;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + absolutePath + "]";
	}

	@Override
	public void write(DocumentElement target, F value) {
		log.tracev( "Ignoring write on document element {}, field '{}' with value '{}'" +
				" because the field was excluded during bootstrap.", target, relativeFieldName, value );
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaCollector;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;

public class StubRootIndexSchemaCollector extends StubIndexSchemaCollector
		implements IndexSchemaCollector {

	private final StubIndexSchemaNode.Builder builder;

	public StubRootIndexSchemaCollector() {
		this( StubIndexSchemaNode.schema() );
	}

	private StubRootIndexSchemaCollector(StubIndexSchemaNode.Builder builder) {
		super( builder );
		this.builder = builder;
	}

	public StubIndexSchemaNode build() {
		return builder.build();
	}
}

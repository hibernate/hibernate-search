/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTerminalContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl.StubIndexFieldReference;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;

class StubIndexSchemaFieldNodeBuilder<F> implements IndexSchemaFieldTerminalContext<IndexFieldReference<F>> {

	private final StubIndexSchemaNode.Builder builder;
	private final boolean included;

	private IndexFieldReference<F> reference;

	StubIndexSchemaFieldNodeBuilder(StubIndexSchemaNode.Builder builder, boolean included) {
		this.builder = builder;
		this.included = included;
	}

	@Override
	public IndexFieldReference<F> toReference() {
		if ( reference == null ) {
			reference = new StubIndexFieldReference<>(
					builder.getAbsolutePath(), builder.getRelativeName(), included
			);
		}
		return reference;
	}
}

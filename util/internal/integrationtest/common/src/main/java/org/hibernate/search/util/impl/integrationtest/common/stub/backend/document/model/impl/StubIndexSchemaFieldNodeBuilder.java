/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTerminalContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl.StubExcludedIndexFieldAccessor;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl.StubIncludedIndexFieldAccessor;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;

class StubIndexSchemaFieldNodeBuilder<F> implements IndexSchemaFieldTerminalContext<IndexFieldAccessor<F>> {

	private final StubIndexSchemaNode.Builder builder;
	private final boolean included;

	private IndexFieldAccessor<F> accessor;

	StubIndexSchemaFieldNodeBuilder(StubIndexSchemaNode.Builder builder, boolean included) {
		this.builder = builder;
		this.included = included;
	}

	@Override
	public IndexFieldAccessor<F> createAccessor() {
		if ( accessor == null ) {
			if ( included ) {
				accessor = new StubIncludedIndexFieldAccessor<>(
						builder.getAbsolutePath(), builder.getRelativeName()
				);
			}
			else {
				accessor = new StubExcludedIndexFieldAccessor<>(
						builder.getAbsolutePath(), builder.getRelativeName()
				);
			}
		}
		return accessor;
	}
}

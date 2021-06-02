/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.dsl.impl;

import java.util.function.BiConsumer;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl.StubIndexFieldReference;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl.StubIndexValueFieldType;

class StubIndexSchemaValueFieldNodeBuilder<F>
		implements IndexSchemaFieldOptionsStep<StubIndexSchemaValueFieldNodeBuilder<F>, IndexFieldReference<F>>,
		StubIndexSchemaFieldBuilder {

	private final StubIndexSchemaDataNode.Builder schemaDataNodeBuilder;
	private final IndexFieldInclusion inclusion;
	private final StubIndexValueFieldType<?> type;

	private IndexFieldReference<F> reference;

	StubIndexSchemaValueFieldNodeBuilder(StubIndexSchemaDataNode.Builder schemaDataNodeBuilder, IndexFieldInclusion inclusion,
			StubIndexValueFieldType<?> type) {
		this.schemaDataNodeBuilder = schemaDataNodeBuilder;
		this.inclusion = inclusion;
		this.type = type;
	}

	@Override
	public StubIndexSchemaValueFieldNodeBuilder<F> multiValued() {
		schemaDataNodeBuilder.multiValued( true );
		return this;
	}

	@Override
	public IndexFieldReference<F> toReference() {
		if ( reference == null ) {
			reference = new StubIndexFieldReference<>(
					schemaDataNodeBuilder.getAbsolutePath(), schemaDataNodeBuilder.getRelativeName(), inclusion
			);
		}
		return reference;
	}

	@Override
	public StubIndexNode build(BiConsumer<String, StubIndexNode> fieldCollector) {
		return new StubIndexNode( schemaDataNodeBuilder.build(), type, null );
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl.impl;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;

class IndexSchemaObjectFieldImpl extends IndexSchemaElementImpl<IndexSchemaObjectFieldNodeBuilder>
		implements IndexSchemaObjectField {

	IndexSchemaObjectFieldImpl(IndexFieldTypeFactory typeFactory,
			IndexSchemaObjectFieldNodeBuilder objectFieldBuilder,
			IndexSchemaNestingContext nestingContext,
			boolean directChildrenAreMultiValuedByDefault) {
		super( typeFactory, objectFieldBuilder, nestingContext, directChildrenAreMultiValuedByDefault );
	}

	@Override
	public IndexSchemaObjectField multiValued() {
		objectNodeBuilder.multiValued();
		return this;
	}

	@Override
	public IndexObjectFieldReference toReference() {
		return objectNodeBuilder.toReference();
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaNestingContext;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaObjectField;
import org.hibernate.search.integrationtest.util.common.stub.backend.document.model.StubIndexSchemaNode;

class StubIndexSchemaObjectField extends StubIndexSchemaElement implements IndexSchemaObjectField {

	private final boolean included;
	private IndexObjectFieldAccessor accessor;

	StubIndexSchemaObjectField(StubIndexSchemaNode.Builder builder,
			IndexSchemaNestingContext context, boolean included) {
		super( builder, context );
		this.included = included;
	}

	@Override
	public IndexObjectFieldAccessor createAccessor() {
		if ( accessor == null ) {
			if ( included ) {
				accessor = new StubIncludedIndexObjectFieldAccessor(
						builder.getAbsolutePath(), builder.getRelativeName()
				);
			}
			else {
				accessor = new StubExcludedIndexObjectFieldAccessor(
						builder.getAbsolutePath(), builder.getRelativeName()
				);
			}
		}
		return accessor;
	}
}

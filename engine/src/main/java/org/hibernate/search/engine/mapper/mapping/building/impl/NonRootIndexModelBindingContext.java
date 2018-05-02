/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectNodeBuilder;
import org.hibernate.search.util.AssertionFailure;

class NonRootIndexModelBindingContext
		extends AbstractIndexModelBindingContext<IndexSchemaObjectNodeBuilder> {
	private final Collection<IndexObjectFieldAccessor> parentObjectAccessors;

	NonRootIndexModelBindingContext(IndexSchemaObjectNodeBuilder indexSchemaObjectNodeBuilder,
			Collection<IndexObjectFieldAccessor> parentObjectAccessors,
			IndexSchemaNestingContextImpl nestingContext) {
		super( indexSchemaObjectNodeBuilder, nestingContext );
		this.parentObjectAccessors = Collections.unmodifiableCollection( parentObjectAccessors );
	}

	@Override
	public Collection<IndexObjectFieldAccessor> getParentIndexObjectAccessors() {
		return parentObjectAccessors;
	}

	@Override
	public void explicitRouting() {
		throw new AssertionFailure(
				"explicitRouting() was called on a non-root binding context; this should never happen."
		);
	}

}

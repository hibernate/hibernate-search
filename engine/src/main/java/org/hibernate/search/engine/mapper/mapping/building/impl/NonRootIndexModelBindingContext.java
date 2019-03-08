/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectNodeBuilder;
import org.hibernate.search.util.common.AssertionFailure;

class NonRootIndexModelBindingContext
		extends AbstractIndexModelBindingContext<IndexSchemaObjectNodeBuilder> {
	private final Collection<IndexObjectFieldReference> parentIndexObjectReferences;

	NonRootIndexModelBindingContext(IndexSchemaRootNodeBuilder indexSchemaRootNodeBuilder,
			IndexSchemaObjectNodeBuilder indexSchemaObjectNodeBuilder,
			Collection<IndexObjectFieldReference> parentIndexObjectReferences,
			ConfiguredIndexSchemaNestingContext nestingContext) {
		super( indexSchemaRootNodeBuilder, indexSchemaObjectNodeBuilder, nestingContext );
		this.parentIndexObjectReferences = Collections.unmodifiableCollection( parentIndexObjectReferences );
	}

	@Override
	public Collection<IndexObjectFieldReference> getParentIndexObjectReferences() {
		return parentIndexObjectReferences;
	}

	@Override
	public void idDslConverter(ToDocumentIdentifierValueConverter<?> idConverter) {
		throw new AssertionFailure(
				"idDslConverter(ToDocumentIdentifierValueConverter) was called on a non-root binding context; this should never happen."
		);
	}

	@Override
	public void explicitRouting() {
		throw new AssertionFailure(
				"explicitRouting() was called on a non-root binding context; this should never happen."
		);
	}

}

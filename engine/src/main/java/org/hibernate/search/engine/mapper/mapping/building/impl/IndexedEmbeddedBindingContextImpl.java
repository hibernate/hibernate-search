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
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexCompositeNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexRootBuilder;
import org.hibernate.search.engine.common.tree.spi.TreeNestingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingMapperContext;

class IndexedEmbeddedBindingContextImpl
		extends AbstractIndexBindingContext<IndexCompositeNodeBuilder>
		implements IndexedEmbeddedBindingContext {
	private final Collection<IndexObjectFieldReference> parentIndexObjectReferences;

	private final boolean parentMultivaluedAndWithoutObjectField;

	IndexedEmbeddedBindingContextImpl(IndexedEntityBindingMapperContext mapperContext,
			IndexRootBuilder indexRootBuilder,
			IndexCompositeNodeBuilder indexCompositeNodeBuilder,
			Collection<IndexObjectFieldReference> parentIndexObjectReferences,
			TreeNestingContext nestingContext,
			boolean parentMultivaluedAndWithoutObjectField) {
		super( mapperContext, indexRootBuilder, indexCompositeNodeBuilder, nestingContext );
		this.parentIndexObjectReferences = Collections.unmodifiableCollection( parentIndexObjectReferences );
		this.parentMultivaluedAndWithoutObjectField = parentMultivaluedAndWithoutObjectField;
	}

	@Override
	public Collection<IndexObjectFieldReference> parentIndexObjectReferences() {
		return parentIndexObjectReferences;
	}

	@Override
	boolean isParentMultivaluedAndWithoutObjectField() {
		return parentMultivaluedAndWithoutObjectField;
	}
}

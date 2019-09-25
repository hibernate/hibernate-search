/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedBindingContext;

class IndexedEmbeddedBindingContextImpl
		extends AbstractIndexBindingContext<IndexSchemaObjectNodeBuilder>
		implements IndexedEmbeddedBindingContext {
	private final Collection<IndexObjectFieldReference> parentIndexObjectReferences;

	private final IndexedEmbeddedPathTracker pathTracker;

	private final boolean parentMultivaluedAndWithoutObjectField;

	IndexedEmbeddedBindingContextImpl(IndexSchemaRootNodeBuilder indexSchemaRootNodeBuilder,
			IndexSchemaObjectNodeBuilder indexSchemaObjectNodeBuilder,
			Collection<IndexObjectFieldReference> parentIndexObjectReferences,
			ConfiguredIndexSchemaNestingContext nestingContext,
			IndexedEmbeddedPathTracker pathTracker,
			boolean parentMultivaluedAndWithoutObjectField) {
		super( indexSchemaRootNodeBuilder, indexSchemaObjectNodeBuilder, nestingContext );
		this.parentIndexObjectReferences = Collections.unmodifiableCollection( parentIndexObjectReferences );
		this.pathTracker = pathTracker;
		this.parentMultivaluedAndWithoutObjectField = parentMultivaluedAndWithoutObjectField;
	}

	@Override
	public Collection<IndexObjectFieldReference> getParentIndexObjectReferences() {
		return parentIndexObjectReferences;
	}

	@Override
	public Set<String> getUselessIncludePaths() {
		return pathTracker.getUselessIncludePaths();
	}

	@Override
	public Set<String> getEncounteredFieldPaths() {
		return pathTracker.getEncounteredFieldPaths();
	}

	@Override
	boolean isParentMultivaluedAndWithoutObjectField() {
		return parentMultivaluedAndWithoutObjectField;
	}
}

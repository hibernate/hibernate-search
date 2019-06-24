/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.impl.IndexSchemaElementImpl;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexSchemaContributionListener;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedBindingContext;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

abstract class AbstractIndexBindingContext<B extends IndexSchemaObjectNodeBuilder> implements IndexBindingContext {

	private final IndexSchemaRootNodeBuilder indexSchemaRootNodeBuilder;
	final B indexSchemaObjectNodeBuilder;
	final ConfiguredIndexSchemaNestingContext nestingContext;

	AbstractIndexBindingContext(IndexSchemaRootNodeBuilder indexSchemaRootNodeBuilder,
			B indexSchemaObjectNodeBuilder, ConfiguredIndexSchemaNestingContext nestingContext) {
		this.indexSchemaRootNodeBuilder = indexSchemaRootNodeBuilder;
		this.indexSchemaObjectNodeBuilder = indexSchemaObjectNodeBuilder;
		this.nestingContext = nestingContext;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "indexSchemaObjectNodeBuilder=" ).append( indexSchemaObjectNodeBuilder )
				.append( ",nestingContext=" ).append( nestingContext )
				.append( "]" )
				.toString();
	}

	@Override
	public IndexFieldTypeFactory createTypeFactory(IndexFieldTypeDefaultsProvider defaultsProvider) {
		return indexSchemaRootNodeBuilder.createTypeFactory( defaultsProvider );
	}

	@Override
	public IndexSchemaElement getSchemaElement() {
		return new IndexSchemaElementImpl<>(
				createTypeFactory(),
				indexSchemaObjectNodeBuilder,
				nestingContext,
				isParentMultivaluedAndWithoutObjectField()
		);
	}

	@Override
	public IndexSchemaElement getSchemaElement(IndexSchemaContributionListener listener) {
		return new IndexSchemaElementImpl<>(
				createTypeFactory(),
				indexSchemaObjectNodeBuilder,
				new NotifyingNestingContext( nestingContext, listener ),
				isParentMultivaluedAndWithoutObjectField()
		);
	}

	@Override
	public Optional<IndexedEmbeddedBindingContext> addIndexedEmbeddedIfIncluded(
			MappableTypeModel parentTypeModel, boolean multiValued,
			String relativePrefix, ObjectFieldStorage storage, Integer maxDepth, Set<String> includePaths) {
		return nestingContext.addIndexedEmbeddedIfIncluded(
				parentTypeModel, relativePrefix, maxDepth, includePaths,
				new NestedContextBuilderImpl(
						indexSchemaRootNodeBuilder, indexSchemaObjectNodeBuilder, storage,
						isParentMultivaluedAndWithoutObjectField() || multiValued
				)
		);
	}

	/**
	 * @return {@code true} if the parent IndexedEmbedded was multi-valued,
	 * and didn't add any object field.
	 * This means in particular that any field added in this context will have to be considered multi-valued,
	 * because it may be contributed multiple times from multiple parent values.
	 */
	abstract boolean isParentMultivaluedAndWithoutObjectField();

	private static class NestedContextBuilderImpl
			implements ConfiguredIndexSchemaNestingContext.NestedContextBuilder<IndexedEmbeddedBindingContext> {

		private final IndexSchemaRootNodeBuilder indexSchemaRootNodeBuilder;
		private IndexSchemaObjectNodeBuilder currentNodeBuilder;
		private final ObjectFieldStorage storage;
		private final List<IndexObjectFieldReference> parentIndexObjectReferences = new ArrayList<>();
		private boolean multiValued;

		private NestedContextBuilderImpl(IndexSchemaRootNodeBuilder indexSchemaRootNodeBuilder,
				IndexSchemaObjectNodeBuilder currentNodeBuilder, ObjectFieldStorage storage,
				boolean multiValued) {
			this.indexSchemaRootNodeBuilder = indexSchemaRootNodeBuilder;
			this.currentNodeBuilder = currentNodeBuilder;
			this.storage = storage;
			this.multiValued = multiValued;
		}

		@Override
		public void appendObject(String objectName) {
			IndexSchemaObjectFieldNodeBuilder nextNodeBuilder =
					currentNodeBuilder.addObjectField( objectName, storage );
			if ( multiValued ) {
				// Only mark the first object as multi-valued
				multiValued = false;
				nextNodeBuilder.multiValued();
			}
			parentIndexObjectReferences.add( nextNodeBuilder.toReference() );
			currentNodeBuilder = nextNodeBuilder;
		}

		@Override
		public IndexedEmbeddedBindingContext build(ConfiguredIndexSchemaNestingContext nestingContext) {
			return new IndexedEmbeddedBindingContextImpl(
					indexSchemaRootNodeBuilder,
					currentNodeBuilder, parentIndexObjectReferences, nestingContext,
					multiValued
			);
		}
	}

}

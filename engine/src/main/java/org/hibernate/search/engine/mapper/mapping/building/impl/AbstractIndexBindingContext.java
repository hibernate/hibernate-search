/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.impl.IndexSchemaElementImpl;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexCompositeNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexObjectFieldBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexRootBuilder;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.engine.common.tree.spi.TreeContributionListener;
import org.hibernate.search.engine.common.tree.spi.TreeFilterPathTracker;
import org.hibernate.search.engine.common.tree.spi.TreeNestingContext;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingMapperContext;
import org.hibernate.search.engine.mapper.model.spi.MappingElement;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

abstract class AbstractIndexBindingContext<B extends IndexCompositeNodeBuilder> implements IndexBindingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final BiFunction<MappingElement, String, SearchException> CYCLIC_RECURSION_EXCEPTION_FACTORY =
			(mappingElement, cyclicRecursionPath) -> log.indexedEmbeddedCyclicRecursion( mappingElement,
					mappingElement.eventContext(), cyclicRecursionPath );

	private final IndexedEntityBindingMapperContext mapperContext;
	private final IndexRootBuilder indexRootBuilder;
	final B indexSchemaObjectNodeBuilder;
	final TreeNestingContext nestingContext;

	AbstractIndexBindingContext(IndexedEntityBindingMapperContext mapperContext,
			IndexRootBuilder indexRootBuilder,
			B indexSchemaObjectNodeBuilder, TreeNestingContext nestingContext) {
		this.mapperContext = mapperContext;
		this.indexRootBuilder = indexRootBuilder;
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
		return indexRootBuilder.createTypeFactory( defaultsProvider );
	}

	@Override
	public IndexSchemaElement schemaElement() {
		return new IndexSchemaElementImpl<>(
				createTypeFactory(),
				indexSchemaObjectNodeBuilder,
				nestingContext,
				isParentMultivaluedAndWithoutObjectField()
		);
	}

	@Override
	public IndexSchemaElement schemaElement(TreeContributionListener listener) {
		return new IndexSchemaElementImpl<>(
				createTypeFactory(),
				indexSchemaObjectNodeBuilder,
				TreeNestingContext.notifying( nestingContext, listener ),
				isParentMultivaluedAndWithoutObjectField()
		);
	}

	@Override
	public Optional<IndexedEmbeddedBindingContext> addIndexedEmbeddedIfIncluded(MappingElement mappingElement,
			String relativePrefix, ObjectStructure structure, TreeFilterDefinition filter, boolean multiValued) {
		TreeFilterPathTracker pathTracker = mapperContext.getOrCreatePathTracker( mappingElement, filter );
		return nestingContext.nestComposed( mappingElement, relativePrefix,
				filter, pathTracker,
				new NestedContextBuilderImpl(
						mapperContext,
						indexRootBuilder, indexSchemaObjectNodeBuilder,
						structure,
						isParentMultivaluedAndWithoutObjectField() || multiValued
				),
				CYCLIC_RECURSION_EXCEPTION_FACTORY
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
			implements TreeNestingContext.NestedContextBuilder<IndexedEmbeddedBindingContext> {

		private final IndexedEntityBindingMapperContext mapperContext;
		private final IndexRootBuilder indexRootBuilder;
		private IndexCompositeNodeBuilder currentNodeBuilder;
		private final ObjectStructure structure;
		private final List<IndexObjectFieldReference> parentIndexObjectReferences = new ArrayList<>();
		private boolean multiValued;

		private NestedContextBuilderImpl(IndexedEntityBindingMapperContext mapperContext,
				IndexRootBuilder indexRootBuilder,
				IndexCompositeNodeBuilder currentNodeBuilder,
				ObjectStructure structure,
				boolean multiValued) {
			this.mapperContext = mapperContext;
			this.indexRootBuilder = indexRootBuilder;
			this.currentNodeBuilder = currentNodeBuilder;
			this.structure = structure;
			this.multiValued = multiValued;
		}

		@Override
		public void appendObject(String objectName) {
			IndexObjectFieldBuilder nextNodeBuilder =
					currentNodeBuilder.addObjectField( objectName, TreeNodeInclusion.INCLUDED, structure );
			if ( multiValued ) {
				// Only mark the first object as multi-valued
				multiValued = false;
				nextNodeBuilder.multiValued();
			}
			parentIndexObjectReferences.add( nextNodeBuilder.toReference() );
			currentNodeBuilder = nextNodeBuilder;
		}

		@Override
		public IndexedEmbeddedBindingContext build(TreeNestingContext nestingContext) {
			return new IndexedEmbeddedBindingContextImpl(
					mapperContext,
					indexRootBuilder,
					currentNodeBuilder, parentIndexObjectReferences, nestingContext,
					multiValued
			);
		}
	}

}

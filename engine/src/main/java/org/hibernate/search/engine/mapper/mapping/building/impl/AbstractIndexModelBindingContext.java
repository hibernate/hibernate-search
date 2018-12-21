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

import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.impl.IndexSchemaElementImpl;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexSchemaContributionListener;
import org.hibernate.search.engine.mapper.model.spi.SearchModel;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

abstract class AbstractIndexModelBindingContext<B extends IndexSchemaObjectNodeBuilder> implements IndexModelBindingContext {

	private final IndexSchemaRootNodeBuilder indexSchemaRootNodeBuilder;
	final B indexSchemaObjectNodeBuilder;
	private final ConfiguredIndexSchemaNestingContext nestingContext;
	private final SearchModel searchModel = new SearchModel() {
		// TODO provide an actual implementation when the interface defines methods
	};

	AbstractIndexModelBindingContext(IndexSchemaRootNodeBuilder indexSchemaRootNodeBuilder,
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
	public IndexFieldTypeFactoryContext getTypeFactory() {
		return indexSchemaRootNodeBuilder.getTypeFactory();
	}

	@Override
	public IndexSchemaElement getSchemaElement() {
		return new IndexSchemaElementImpl<>(
				getTypeFactory(),
				indexSchemaObjectNodeBuilder,
				nestingContext
		);
	}

	@Override
	public IndexSchemaElement getSchemaElement(IndexSchemaContributionListener listener) {
		return new IndexSchemaElementImpl<>(
				getTypeFactory(),
				indexSchemaObjectNodeBuilder,
				new NotifyingNestingContext( nestingContext, listener )
		);
	}

	@Override
	public SearchModel getSearchModel() {
		return searchModel;
	}

	@Override
	public Optional<IndexModelBindingContext> addIndexedEmbeddedIfIncluded(MappableTypeModel parentTypeModel,
			String relativePrefix, ObjectFieldStorage storage, Integer maxDepth, Set<String> includePaths) {
		return nestingContext.addIndexedEmbeddedIfIncluded(
				parentTypeModel, relativePrefix, maxDepth, includePaths,
				new NestedContextBuilderImpl( indexSchemaRootNodeBuilder, indexSchemaObjectNodeBuilder, storage )
		);
	}

	private static class NestedContextBuilderImpl
			implements ConfiguredIndexSchemaNestingContext.NestedContextBuilder<IndexModelBindingContext> {

		private final IndexSchemaRootNodeBuilder indexSchemaRootNodeBuilder;
		private IndexSchemaObjectNodeBuilder currentNodeBuilder;
		private final ObjectFieldStorage storage;
		private final List<IndexObjectFieldAccessor> parentObjectAccessors = new ArrayList<>();

		private NestedContextBuilderImpl(IndexSchemaRootNodeBuilder indexSchemaRootNodeBuilder,
				IndexSchemaObjectNodeBuilder currentNodeBuilder, ObjectFieldStorage storage) {
			this.indexSchemaRootNodeBuilder = indexSchemaRootNodeBuilder;
			this.currentNodeBuilder = currentNodeBuilder;
			this.storage = storage;
		}

		@Override
		public void appendObject(String objectName) {
			IndexSchemaObjectFieldNodeBuilder nextNodeBuilder =
					currentNodeBuilder.addObjectField( objectName, storage );
			parentObjectAccessors.add( nextNodeBuilder.createAccessor() );
			currentNodeBuilder = nextNodeBuilder;
		}

		@Override
		public IndexModelBindingContext build(ConfiguredIndexSchemaNestingContext nestingContext) {
			return new NonRootIndexModelBindingContext(
					indexSchemaRootNodeBuilder,
					currentNodeBuilder, parentObjectAccessors, nestingContext
			);
		}
	}

}

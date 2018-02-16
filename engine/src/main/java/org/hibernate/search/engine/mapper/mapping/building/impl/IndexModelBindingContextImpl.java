/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaCollector;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaNestingContext;
import org.hibernate.search.engine.backend.document.model.spi.ObjectFieldIndexSchemaCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.model.SearchModel;
import org.hibernate.search.engine.mapper.model.spi.IndexableTypeOrdering;
import org.hibernate.search.engine.mapper.model.spi.IndexedTypeIdentifier;

public class IndexModelBindingContextImpl implements IndexModelBindingContext {

	private final IndexSchemaCollector schemaCollector;
	private final Iterable<IndexObjectFieldAccessor> parentObjectAccessors;
	private final IndexSchemaNestingContextImpl nestingContext;
	private final SearchModel searchModel = new SearchModel() {
		// TODO provide an actual implementation when the interface defines methods
	};

	private IndexSchemaElement schemaElementWithNestingContext;

	public IndexModelBindingContextImpl(IndexSchemaCollector schemaCollector,
			IndexableTypeOrdering typeOrdering) {
		this( schemaCollector, Collections.emptyList(), new IndexSchemaNestingContextImpl( typeOrdering ) );
	}

	private IndexModelBindingContextImpl(IndexSchemaCollector schemaCollector,
			Iterable<IndexObjectFieldAccessor> parentObjectAccessors,
			IndexSchemaNestingContextImpl nestingContext) {
		this.schemaCollector = schemaCollector;
		this.parentObjectAccessors = parentObjectAccessors;
		this.nestingContext = nestingContext;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "schemaCollector=" ).append( schemaCollector )
				.append( ",nestingContext=" ).append( nestingContext )
				.append( "]" )
				.toString();
	}

	@Override
	public Iterable<IndexObjectFieldAccessor> getParentIndexObjectAccessors() {
		return parentObjectAccessors;
	}

	@Override
	public IndexSchemaElement getSchemaElement() {
		if ( schemaElementWithNestingContext == null ) {
			schemaElementWithNestingContext = schemaCollector.withContext( nestingContext );
		}
		return schemaElementWithNestingContext;
	}

	@Override
	public SearchModel getSearchModel() {
		return searchModel;
	}

	@Override
	public void explicitRouting() {
		schemaCollector.explicitRouting();
	}

	@Override
	public Optional<IndexModelBindingContext> addIndexedEmbeddedIfIncluded(IndexedTypeIdentifier parentTypeId,
			String relativePrefix, ObjectFieldStorage storage, Integer maxDepth, Set<String> includePaths) {
		return nestingContext.addIndexedEmbeddedIfIncluded(
				parentTypeId, relativePrefix, maxDepth, includePaths,
				new Builder( schemaCollector, storage )
		);
	}


	private static class Builder
			implements IndexSchemaNestingContextImpl.NestedContextBuilder<IndexModelBindingContext> {

		private IndexSchemaCollector currentIndexModelNode;
		private final ObjectFieldStorage storage;
		private final List<IndexObjectFieldAccessor> parentObjectAccessors = new ArrayList<>();

		private Builder(IndexSchemaCollector indexModelNode, ObjectFieldStorage storage) {
			this.currentIndexModelNode = indexModelNode;
			this.storage = storage;
		}

		@Override
		public void appendObject(String objectName) {
			ObjectFieldIndexSchemaCollector nextNode = currentIndexModelNode.objectField( objectName, storage );
			parentObjectAccessors.add( nextNode.withContext( IndexSchemaNestingContext.includeAll() ).createAccessor() );
			currentIndexModelNode = nextNode;
		}

		@Override
		public IndexModelBindingContext build(IndexSchemaNestingContextImpl nestingContext) {
			return new IndexModelBindingContextImpl( currentIndexModelNode, parentObjectAccessors, nestingContext );
		}
	}

}

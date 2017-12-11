/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.model.spi.IndexableTypeOrdering;
import org.hibernate.search.engine.mapper.model.spi.IndexedTypeIdentifier;

public class IndexModelBindingContextImpl implements IndexModelBindingContext {

	private final IndexSchemaCollector schemaCollector;
	private final IndexModelNestingContextImpl nestingContext;

	private IndexSchemaElement schemaElementWithNestingContext;

	public IndexModelBindingContextImpl(IndexSchemaCollector schemaCollector,
			IndexableTypeOrdering typeOrdering) {
		this( schemaCollector, new IndexModelNestingContextImpl( typeOrdering ) );
	}

	private IndexModelBindingContextImpl(IndexSchemaCollector schemaCollector,
			IndexModelNestingContextImpl nestingContext) {
		this.schemaCollector = schemaCollector;
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
	public IndexSchemaElement getSchemaElement() {
		if ( schemaElementWithNestingContext == null ) {
			schemaElementWithNestingContext = schemaCollector.withContext( nestingContext );
		}
		return schemaElementWithNestingContext;
	}

	@Override
	public void explicitRouting() {
		schemaCollector.explicitRouting();
	}

	@Override
	public Optional<IndexModelBindingContext> addIndexedEmbeddedIfIncluded(IndexedTypeIdentifier parentTypeId,
			String relativePrefix, Integer nestedMaxDepth, Set<String> nestedPathFilters) {
		return nestingContext.addIndexedEmbeddedIfIncluded(
				relativePrefix,
				f -> f.composeWithNested( parentTypeId, relativePrefix, nestedMaxDepth, nestedPathFilters ),
				schemaCollector, IndexSchemaCollector::childObject,
				IndexModelBindingContextImpl::new
		);
	}

}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.index.spi.SearchTarget;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSearchTarget;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSessionContext;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.dsl.SearchResultDefinitionContext;
import org.hibernate.search.util.AssertionFailure;

public class PojoSearchTargetImpl implements PojoSearchTarget {

	private final PojoTypeManagerContainer typeManagers;
	private final Set<PojoTypeManager<?, ?, ?>> targetedTypeManagers;

	public PojoSearchTargetImpl(PojoTypeManagerContainer typeManagers,
			Set<PojoTypeManager<?, ?, ?>> targetedTypeManagers) {
		this.typeManagers = typeManagers;
		this.targetedTypeManagers = targetedTypeManagers;
	}

	@Override
	public Set<Class<?>> getTargetedIndexedTypes() {
		return targetedTypeManagers.stream()
				.map( PojoTypeManager::getEntityType )
				.collect( Collectors.toCollection( LinkedHashSet::new) );
	}

	@Override
	public SearchResultDefinitionContext<PojoReference> search(
			PojoSessionContext context) {
		return createIndexSearchTarget().search( context, this::toPojoReference );
	}

	private SearchTarget createIndexSearchTarget() {
		return targetedTypeManagers.stream()
				.map( PojoTypeManager::createSearchTarget )
				.reduce( (a, b) -> {
					a.add( b );
					return a;
				} )
				// If we get here, there is at least one type manager
				.get();
	}

	private PojoReference toPojoReference(DocumentReference documentReference) {
		PojoTypeManager<?, ?, ?> typeManager = typeManagers.getByIndexName( documentReference.getIndexName() )
				.orElseThrow( () -> new AssertionFailure(
						"Document reference " + documentReference + " could not be converted to a PojoReference" ) );
		// TODO error handling if typeManager is null
		Object id = typeManager.getIdentifierMapping().fromDocumentId( documentReference.getId() );
		return new PojoReferenceImpl( typeManager.getEntityType(), id );
	}
}

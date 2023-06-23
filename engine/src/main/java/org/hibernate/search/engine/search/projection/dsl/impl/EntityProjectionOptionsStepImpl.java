/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.backend.reporting.spi.BackendMappingHints;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.EntityProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.projection.spi.ProjectionMappedTypeContext;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionIndexScope;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class EntityProjectionOptionsStepImpl<E>
		implements EntityProjectionOptionsStep<EntityProjectionOptionsStepImpl<E>, E> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchProjectionIndexScope<?> scope;
	private final SearchProjectionFactory<?, ?> projectionFactory;
	private final Class<E> requestedEntityType;

	public EntityProjectionOptionsStepImpl(SearchProjectionDslContext<?> dslContext,
			SearchProjectionFactory<?, ?> projectionFactory, Class<E> requestedEntityType) {
		this.scope = dslContext.scope();
		this.projectionFactory = projectionFactory;
		this.requestedEntityType = requestedEntityType;
	}

	@Override
	public SearchProjection<E> toProjection() {
		List<? extends ProjectionMappedTypeContext> mappedTypeContexts = scope.mappedTypeContexts();
		boolean canUseProjectionFromFirst = true;
		ProjectionMappedTypeContext first = mappedTypeContexts.get( 0 );
		for ( ProjectionMappedTypeContext mappedTypeContext : mappedTypeContexts ) {
			boolean willYieldSameProjectionAsFirst = first.loadingAvailable()
					? mappedTypeContext.loadingAvailable()
					: first.javaClass().equals( mappedTypeContext.javaClass() );
			if ( !willYieldSameProjectionAsFirst ) {
				canUseProjectionFromFirst = false;
				break;
			}
		}
		if ( canUseProjectionFromFirst ) {
			return toProjection( first );
		}
		Map<String, SearchProjection<? extends E>> byTypeName = new HashMap<>();
		for ( ProjectionMappedTypeContext mappedTypeContext : mappedTypeContexts ) {
			byTypeName.put( mappedTypeContext.name(), toProjection( mappedTypeContext ) );
		}
		return scope.projectionBuilders().byTypeName( byTypeName );
	}

	// The casts are safe because a query making use of this projection can only target entity types extending E
	@SuppressWarnings({ "unchecked" })
	private SearchProjection<E> toProjection(ProjectionMappedTypeContext mappedTypeContext) {
		if ( requestedEntityType != null && !requestedEntityType.isAssignableFrom( mappedTypeContext.javaClass() ) ) {
			throw log.invalidTypeForEntityProjection( mappedTypeContext.name(), mappedTypeContext.javaClass(),
					requestedEntityType
			);
		}
		if ( mappedTypeContext.loadingAvailable() ) {
			return scope.projectionBuilders().entityLoading();
		}
		else {
			Class<?> javaClass = mappedTypeContext.javaClass();
			if ( scope.projectionRegistry().compositeOptional( javaClass ).isPresent() ) {
				return scope.projectionBuilders().entityComposite(
						(SearchProjection<E>) projectionFactory.composite().as( javaClass ).toProjection()
				);
			}
		}
		// If the projection is impossible, we delay the exception until we actually have to project a document.
		// The entity projection is the default projection so if we threw the exception immediately,
		// we would prevent use cases such as .search(MyEntity.class).where(...).fetchTotalHitCount(),
		// where the projection is impossible but it's just the default so it's not the user's fault,
		// and in the end does not matter because the projection is never executed anyway.
		BackendMappingHints hints = scope.mappingContext().hints();
		return scope.projectionBuilders().throwing( () -> log.cannotCreateEntityProjection(
				mappedTypeContext.name(), mappedTypeContext.javaClass(),
				hints.noEntityProjectionAvailable() ) );
	}

}

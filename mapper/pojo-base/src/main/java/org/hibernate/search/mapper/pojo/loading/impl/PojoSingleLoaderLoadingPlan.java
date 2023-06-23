/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.loading.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingStrategy;

final class PojoSingleLoaderLoadingPlan<T> implements PojoLoadingPlan<T> {

	private final PojoSelectionLoadingContext context;
	private final PojoSelectionLoadingStrategy<T> loadingStrategy;

	private final Set<PojoLoadingTypeContext<? extends T>> expectedTypes = new LinkedHashSet<>();
	private final List<Object> identifiers = new ArrayList<>();

	private boolean singleConcreteTypeInEntityHierarchy;
	private List<T> loaded;

	PojoSingleLoaderLoadingPlan(PojoSelectionLoadingContext context,
			PojoSelectionLoadingStrategy<T> loadingStrategy) {
		this.context = context;
		this.loadingStrategy = loadingStrategy;
	}

	@Override
	public <T2 extends T> int planLoading(PojoLoadingTypeContext<T2> expectedType, Object identifier) {
		expectedTypes.add( expectedType );
		identifiers.add( identifier );
		return identifiers.size() - 1;
	}

	@Override
	public void loadBlocking(Deadline deadline) {
		context.checkOpen();
		if ( identifiers.isEmpty() ) {
			// Avoid creating and calling the loader:
			// it may be expensive even if there are no entities to load,
			// and we don't expect any call to getLoaded*() in this case.
			return;
		}
		try {
			PojoSelectionEntityLoader<T> loader = loadingStrategy.createLoader( expectedTypes );
			singleConcreteTypeInEntityHierarchy = expectedTypes.size() == 1
					&& expectedTypes.iterator().next().isSingleConcreteTypeInEntityHierarchy();
			loaded = loader.loadBlocking( identifiers, deadline );
		}
		finally {
			expectedTypes.clear();
			identifiers.clear();
		}
	}

	@Override
	public <T2 extends T> T2 retrieve(PojoLoadingTypeContext<T2> expectedType, int ordinal) {
		T retrieved = loaded.get( ordinal );
		if ( retrieved == null ) {
			return null; // Couldn't be loaded.
		}
		return castToExactTypeOrNull( expectedType, retrieved );
	}

	@Override
	public void clear() {
		expectedTypes.clear();
		identifiers.clear();
		loaded = null;
	}

	/**
	 * Casts a loaded entity returned by {@link PojoSelectionEntityLoader#loadBlocking(List, Deadline)}
	 * to the expected type, or returns {@code null} if it has an unexpected type.
	 * <p>
	 * Under some circumstances, the loading may return entities that do not have the exact type expected by users.
	 * <p>
	 * For example, let's consider entity types A, B, C, D, with B, C, and D extending A
	 * Let's imagine an instance of type B and with id 4 is deleted from the database
	 * and replaced with an instance of type D and id 4.
	 * If a search on entity types B and C is performed before the index is refreshed,
	 * we might be requested to load entity B with id 4,
	 * and since we're working with the common supertype A,
	 * loading will succeed but will yield an entity of type D with id 4.
	 * <p>
	 * Now, the entity will still be an instance of A, but... the user doesn't care about A:
	 * the user asked for a search on entities B and C.
	 * Returning D might be a problem, especially if the user intends to call methods defined on an interface I,
	 * implemented by B and C, but not D.
	 * This will be a problem since that entity does not implement I.
	 * <p>
	 * The easiest way to avoid this problem is to just check the type of every loaded entity,
	 * to be sure it's the same type that was originally requested.
	 * Then we will be safe, because callers are expected to only pass entity references
	 * to types that were originally targeted by the search,
	 * and these types are known to implement any interface that the user could possibly rely on.
	 *
	 * @param <T2> The expected type for the entity instance.
	 * @param expectedType The expected type for the entity instance. Must be one of the types passed
	 * to {@link PojoSelectionLoadingStrategy#createLoader(Set)}
	 * when creating this loader.
	 * @param loadedObject A loaded object, i.e. an element from {@link #loaded}.
	 * @return The given {@code loadedObject} if is an instance of {@code expectedType} exactly (not an instance of a subtype).
	 * {@code null} otherwise.
	 */
	// The cast is safe because we use reflection to check it.
	@SuppressWarnings("unchecked")
	private <T2 extends T> T2 castToExactTypeOrNull(PojoLoadingTypeContext<T2> expectedType, T loadedObject) {
		if ( singleConcreteTypeInEntityHierarchy ) {
			// The loaded object will always be an instance of the exact same type,
			// and we can only get passed that exact type.
			return (T2) loadedObject;
		}
		else if ( expectedType.typeIdentifier().equals( context.runtimeIntrospector().detectEntityType( loadedObject ) ) ) {
			return (T2) loadedObject;
		}
		else {
			return null;
		}
	}

}

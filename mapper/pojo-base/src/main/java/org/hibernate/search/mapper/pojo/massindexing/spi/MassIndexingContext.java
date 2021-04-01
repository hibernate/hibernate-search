/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.spi;

import java.util.List;
import org.hibernate.search.mapper.pojo.loading.LoadingInterceptor;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingEntityLoadingStrategy;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

/**
 * Contextual information about a mass indexing proccess.
 * @param <O> The options for mass indexing proccess.
 */
public interface MassIndexingContext<O> {

	/**
	 * @param <T> The exposed type of indexed entities.
	 * @param expectedType The expected types of indexed objects.
	 * @return A index loader.
	 * @see MassIndexingContext
	 */
	<T> MassIndexingEntityLoadingStrategy<T, O> createIndexLoadingStrategy(PojoRawTypeIdentifier<? extends T> expectedType);

	/**
	 * @param entityType The type of loaded object.
	 * @return A entity name of entity type.
	 */
	String entityName(PojoRawTypeIdentifier<?> entityType);

	/**
	 * @param sessionContext the session context
	 * @param entity the loaded entity
	 * @return A entity name of entity type.
	 */
	Object entityIdentifier(MassIndexingSessionContext sessionContext, Object entity);

	/**
	 * @return A list {@link LoadingInterceptor} of entityIdentifier interceptors.
	 */
	List<? extends LoadingInterceptor<? super O>> identifierInterceptors();

	/**
	 * @return A list {@link LoadingInterceptor} of entity interceptors.
	 */
	List<? extends LoadingInterceptor<? super O>> documentInterceptors();

}

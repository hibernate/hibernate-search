/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.loader;

/**
 * Contextual information about a mass indexing proccess.
 * @param <O> The options for mass indexing proccess.
 */
public interface MassIndexingThreadContext<O> {

	/**
	 * @return The mass options for loading process.
	 */
	O options();

	/**
	 * Get the in progress indexing process,
	 *
	 * @return true if process is active.
	 */
	boolean active();

	/**
	 * Get the context contributed by interceptors.
	 *
	 * @param <T> a context type.
	 * @param contextType a context type;
	 * @return context contributed by interceptors.
	 */
	<T> T context(Class<T> contextType);

	/**
	 * Get the entity super type indexing process.
	 *
	 * @return the entity super type.
	 */
	Class<?> commonSuperType();

	/**
	 * Get the entity name of super type.
	 *
	 * @return the entity name of super type.
	 */
	String commonSuperEntityName();

	/**
	 * @param entity the loaded entity
	 * @return A entity identifier of entity type.
	 */
	Object entityIdentifier(Object entity);

	/**
	 * @param entity the loaded entity
	 * @return A true if entity is indexed.
	 */
	boolean indexed(Object entity);

	/**
	 * Get the entity names indexing process,
	 *
	 * @return the entity names by separated comma list.
	 */
	String includedEntityNames();
}

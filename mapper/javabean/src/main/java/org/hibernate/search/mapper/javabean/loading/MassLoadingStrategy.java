/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.loading;

/**
 * A strategy for mass loading, used in particular during mass indexing.
 *
 * @param <E> The type of loaded entities.
 * @param <I> The type of entity identifiers.
 */
public interface MassLoadingStrategy<E, I> {

	/**
	 * @param obj Another strategy
	 * @return {@code true} if the other strategy targets the same entity hierarchy
	 * and can be used as a replacement for this one.
	 * {@code false} otherwise or when unsure.
	 */
	@Override
	boolean equals(Object obj);

	/*
	 * Hashcode must be overridden to be consistent with equals.
	 */
	@Override
	int hashCode();

	/**
	 * @param includedTypes A representation of all entity types that will have to be loaded.
	 * @param sink A sink to which the entity identifier loader will pass loaded identifiers.
	 * @param options Loading options configured by the requester (who requested mass indexing, ...).
	 * @return An entity identifier loader.
	 */
	MassIdentifierLoader createIdentifierLoader(LoadingTypeGroup<E> includedTypes, MassIdentifierSink<I> sink,
			MassLoadingOptions options);

	/**
	 * @param includedTypes A representation of all entity types that will have to be loaded.
	 * @param sink A sink to which the entity loader will pass loaded entities.
	 * @param options Loading options configured by the requester (who requested mass indexing, ...).
	 * @return An entity loader.
	 */
	MassEntityLoader<I> createEntityLoader(LoadingTypeGroup<E> includedTypes, MassEntitySink<E> sink,
			MassLoadingOptions options);

}

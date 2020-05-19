/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.model.spi;

import java.util.stream.Stream;

/**
 * A representation of an entity type that can be mapped to an index.
 *
 * @see org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider#get(MappableTypeModel)
 * @see org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataDiscoverer#discover(MappableTypeModel)
 */
public interface MappableTypeModel {

	/**
	 * @return A human-readable name for this type.
	 */
	String name();

	/**
	 * @return {@code true} if this type is abstract, i.e. it cannot be instantiated as-is (but may be as a subtype).
	 * {@code false} otherwise.
	 */
	boolean isAbstract();

	/**
	 * @param superTypeCandidate Another type that should be assessed as a supertype.
	 * @return {@code true} if this type is a subtype of {@code superTypeCandidate},
	 * i.e. if {@code superTypeCandidate} is mentioned in {@link #ascendingSuperTypes()}.
	 * {@code false} otherwise.
	 */
	boolean isSubTypeOf(MappableTypeModel superTypeCandidate);

	/**
	 * @return A stream of all supertypes of this type, from the most specific (this type)
	 * to the least specific ({@code Object}).
	 */
	Stream<? extends MappableTypeModel> ascendingSuperTypes();

	/**
	 * @return A stream of all supertypes of this type, from the least specific ({@code Object})
	 * to the most specific (this type).
	 */
	Stream<? extends MappableTypeModel> descendingSuperTypes();

	/**
	 * @return A human-readable description of this type.
	 */
	@Override
	String toString();

	/**
	 * @return {@code true} if {@code obj} is a {@link MappableTypeModel} referencing the exact same type
	 * with the exact same exposed metadata.
	 */
	@Override
	boolean equals(Object obj);

	/*
	 * Note to implementors: you must override hashCode to be consistent with equals().
	 */
	@Override
	int hashCode();

}

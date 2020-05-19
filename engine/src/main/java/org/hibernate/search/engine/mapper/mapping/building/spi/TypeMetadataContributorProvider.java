/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import java.util.Set;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

/**
 * @param <C> The Java type of type metadata contributors
 */
public interface TypeMetadataContributorProvider<C> {

	/**
	 * @param typeModel The model of a type to retrieve contributors for, including supertype contributors.
	 *
	 * @return A set of the Java types of the metadata contributors
	 */
	Set<C> get(MappableTypeModel typeModel);

	/**
	 * @return A set containing all the types that were contributed to so far.
	 */
	Set<? extends MappableTypeModel> typesContributedTo();

}

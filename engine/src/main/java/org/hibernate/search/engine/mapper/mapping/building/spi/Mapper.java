/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.model.spi.IndexedTypeIdentifier;

/**
 * @author Yoann Rodiere
 */
public interface Mapper<C, M extends MappingImplementor> {

	/**
	 * @param typeId The identifier of the indexed type to be mapped
	 * @param indexManagerBuildingState The building state for the index to be mapped
	 * @param contributorProvider A provider of composite mapping contributors, with contributions
	 * guaranteed to be ordered from supertype to subtype, allowing the builder to support overrides if necessary.
	 */
	void addIndexed(IndexedTypeIdentifier typeId,
			IndexManagerBuildingState<?> indexManagerBuildingState,
			TypeMetadataContributorProvider<C> contributorProvider);

	M build();

}

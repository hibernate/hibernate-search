/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.augmented.building.impl.PojoAugmentedTypeModelProvider;

public class PojoMappingHelper {

	private final TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider;
	private final PojoAugmentedTypeModelProvider augmentedTypeModelProvider;
	private final PojoIndexModelBinder indexModelBinder;

	PojoMappingHelper(TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider,
			PojoIndexModelBinder indexModelBinder,
			PojoAugmentedTypeModelProvider augmentedTypeModelProvider) {
		this.contributorProvider = contributorProvider;
		this.indexModelBinder = indexModelBinder;
		this.augmentedTypeModelProvider = augmentedTypeModelProvider;
	}

	public TypeMetadataContributorProvider<PojoTypeMetadataContributor> getContributorProvider() {
		return contributorProvider;
	}

	public PojoIndexModelBinder getIndexModelBinder() {
		return indexModelBinder;
	}

	public PojoAugmentedTypeModelProvider getAugmentedTypeModelProvider() {
		return augmentedTypeModelProvider;
	}

}

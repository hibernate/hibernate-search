/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.augmented.building.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.augmented.impl.PojoAugmentedTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

public class PojoAugmentedTypeModelProvider {

	private final TypeMetadataContributorProvider<PojoTypeMetadataContributor> modelContributorProvider;
	private final Map<PojoRawTypeModel<?>, PojoAugmentedTypeModel> cache = new HashMap<>();

	public PojoAugmentedTypeModelProvider(
			TypeMetadataContributorProvider<PojoTypeMetadataContributor> modelContributorProvider) {
		this.modelContributorProvider = modelContributorProvider;
	}

	public PojoAugmentedTypeModel get(PojoRawTypeModel<?> typeModel) {
		return cache.computeIfAbsent( typeModel, this::createAugmentedTypeModel );
	}

	private PojoAugmentedTypeModel createAugmentedTypeModel(PojoRawTypeModel<?> typeModel) {
		PojoAugmentedTypeModelBuilder builder = new PojoAugmentedTypeModelBuilder();
		modelContributorProvider.forEach( typeModel, c -> c.contributeModel( builder ) );
		return builder.build();
	}

}

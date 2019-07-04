/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoValueAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.engine.reporting.spi.FailureCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

public class PojoTypeAdditionalMetadataProvider {

	private final BeanResolver beanResolver;
	private final FailureCollector failureCollector;
	private final TypeMetadataContributorProvider<PojoTypeMetadataContributor> modelContributorProvider;
	private final Map<PojoRawTypeModel<?>, PojoTypeAdditionalMetadata> cache = new HashMap<>();

	public PojoTypeAdditionalMetadataProvider(BeanResolver beanResolver,
			FailureCollector failureCollector,
			TypeMetadataContributorProvider<PojoTypeMetadataContributor> modelContributorProvider) {
		this.beanResolver = beanResolver;
		this.failureCollector = failureCollector;
		this.modelContributorProvider = modelContributorProvider;
	}

	public PojoTypeAdditionalMetadata get(PojoRawTypeModel<?> typeModel) {
		return cache.computeIfAbsent( typeModel, this::createTypeAdditionalMetadata );
	}

	public PojoValueAdditionalMetadata get(BoundPojoModelPathValueNode<?, ?, ?> valueNode) {
		BoundPojoModelPathPropertyNode<?, ?> propertyNode = valueNode.getParent();
		BoundPojoModelPathTypeNode<?> typeNode = propertyNode.getParent();
		PojoTypeModel<?> typeModel = typeNode.getTypeModel();
		return get( typeModel.getRawType() )
				.getPropertyAdditionalMetadata( propertyNode.getPropertyModel().getName() )
				.getValueAdditionalMetadata( valueNode.getExtractorPath() );
	}

	private PojoTypeAdditionalMetadata createTypeAdditionalMetadata(PojoRawTypeModel<?> typeModel) {
		PojoTypeAdditionalMetadataBuilder builder = new PojoTypeAdditionalMetadataBuilder(
				beanResolver, failureCollector, typeModel
		);
		modelContributorProvider.forEach( typeModel, c -> c.contributeAdditionalMetadata( builder ) );
		return builder.build();
	}
}

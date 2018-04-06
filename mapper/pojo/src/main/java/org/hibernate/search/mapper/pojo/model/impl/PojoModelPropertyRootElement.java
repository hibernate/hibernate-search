/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import java.util.stream.Stream;

import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;


/**
 * @author Yoann Rodiere
 */
public class PojoModelPropertyRootElement extends AbstractPojoModelElement implements PojoModelProperty {

	private final PojoPropertyModel<?> propertyModel;

	public PojoModelPropertyRootElement(PojoPropertyModel<?> propertyModel,
			TypeMetadataContributorProvider<PojoTypeMetadataContributor> modelContributorProvider) {
		super( modelContributorProvider );
		this.propertyModel = propertyModel;
	}

	@Override
	public String toString() {
		return propertyModel.toString();
	}

	@Override
	public PojoModelElementAccessor<?> createAccessor() {
		return new PojoModelRootElementAccessor<>();
	}

	@Override
	public <M> Stream<M> markers(Class<M> markerType) {
		return Stream.empty();
	}

	@Override
	public String getName() {
		return propertyModel.getName();
	}

	@Override
	PojoTypeModel<?> getTypeModel() {
		return propertyModel.getTypeModel();
	}
}

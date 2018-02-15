/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoModelElement;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.SearchException;


/**
 * @author Yoann Rodiere
 */
public class PojoModelRootElement extends AbstractPojoModelElement implements PojoModelElement {

	private final PojoTypeModel<?> typeModel;

	public PojoModelRootElement(PojoTypeModel<?> typeModel,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> modelContributorProvider) {
		super( modelContributorProvider );
		this.typeModel = typeModel;
	}

	@Override
	public <T> PojoModelElementAccessor<T> createAccessor(Class<T> requestedType) {
		Optional<PojoTypeModel<T>> superTypeModel = typeModel.getSuperType( requestedType );
		if ( !superTypeModel.isPresent() ) {
			throw new SearchException( "Requested incompatible type for '" + createAccessor() + "': '" + requestedType + "'" );
		}
		return new PojoModelRootElementAccessor<>( superTypeModel.get() );
	}

	@Override
	public PojoModelElementAccessor<?> createAccessor() {
		return new PojoModelRootElementAccessor<>( typeModel );
	}

	@Override
	public <M> Stream<M> markers(Class<M> markerType) {
		return Stream.empty();
	}

	@Override
	public PojoTypeModel<?> getTypeModel() {
		return typeModel;
	}
}

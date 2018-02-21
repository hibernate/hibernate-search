/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeModelCollector;
import org.hibernate.search.mapper.pojo.model.PojoModelElement;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;


/**
 * @author Yoann Rodiere
 */
abstract class AbstractPojoModelElement implements PojoModelElement, PojoTypeNodeModelCollector {

	private final TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> modelContributorProvider;

	private final Map<String, PojoModelNestedElement> propertyModelsByName = new HashMap<>();

	private boolean markersForTypeInitialized = false;

	AbstractPojoModelElement(TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> modelContributorProvider) {
		this.modelContributorProvider = modelContributorProvider;
	}

	@Override
	public boolean isAssignableTo(Class<?> clazz) {
		return getTypeModel().getSuperType( clazz ).isPresent();
	}

	@Override
	public PojoModelNestedElement property(String relativeName) {
		initMarkersForType();
		return propertyModelsByName.computeIfAbsent( relativeName, name -> {
			PojoPropertyModel<?> model = getTypeModel().getProperty( name );
			return new PojoModelNestedElement( this, model, modelContributorProvider );
		} );
	}

	@SuppressWarnings({ "unchecked", "rawtypes" }) // Stream is covariant in T
	@Override
	public Stream<PojoModelProperty> properties() {
		initMarkersForType();
		return (Stream<PojoModelProperty>) (Stream) propertyModelsByName.values().stream();
	}

	/*
	 * Lazily initialize markers.
	 * Lazy initialization is necessary to avoid infinite recursion.
	 */
	private void initMarkersForType() {
		if ( !markersForTypeInitialized ) {
			this.markersForTypeInitialized = true;
			getModelContributorProvider().get( getTypeModel() )
					.forEach( c -> c.contributeModel( this ) );
		}
	}

	abstract PojoTypeModel<?> getTypeModel();

	private TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> getModelContributorProvider() {
		return modelContributorProvider;
	}

}

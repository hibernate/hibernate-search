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
import org.hibernate.search.mapper.pojo.model.spi.IndexableModel;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeModelCollector;
import org.hibernate.search.mapper.pojo.model.spi.PropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.TypeModel;


/**
 * @author Yoann Rodiere
 */
public abstract class PojoIndexableModel implements IndexableModel, PojoTypeNodeModelCollector {

	private final TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> modelContributorProvider;

	private final Map<String, PojoPropertyIndexableModel> propertyModelsByName = new HashMap<>();

	private boolean markersForTypeInitialized = false;

	public PojoIndexableModel(TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> modelContributorProvider) {
		this.modelContributorProvider = modelContributorProvider;
	}

	@Override
	public abstract PojoIndexableReference<?> asReference();

	@Override
	public boolean isAssignableTo(Class<?> clazz) {
		return clazz.isAssignableFrom( getJavaType() );
	}

	@Override
	public PojoPropertyIndexableModel property(String relativeName) {
		initMarkersForType();
		return propertyModelsByName.computeIfAbsent( relativeName, name -> {
			PropertyModel<?> model = getTypeModel().getProperty( name );
			return new PojoPropertyIndexableModel( this, model, modelContributorProvider );
		} );
	}

	@SuppressWarnings({ "unchecked", "rawtypes" }) // Stream is covariant in T
	@Override
	public Stream<IndexableModel> properties() {
		initMarkersForType();
		return (Stream<IndexableModel>) (Stream) propertyModelsByName.values().stream();
	}

	/*
	 * Lazily initialize markers.
	 * Lazy initialization is necessary to avoid inifinite recursion.
	 */
	private void initMarkersForType() {
		if ( !markersForTypeInitialized ) {
			this.markersForTypeInitialized = true;
			getModelContributorProvider().get( new PojoIndexedTypeIdentifier( getJavaType() ) )
					.forEach( c -> c.contributeModel( this ) );
		}
	}

	protected abstract TypeModel<?> getTypeModel();

	protected abstract Class<?> getJavaType();

	protected TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> getModelContributorProvider() {
		return modelContributorProvider;
	}

}

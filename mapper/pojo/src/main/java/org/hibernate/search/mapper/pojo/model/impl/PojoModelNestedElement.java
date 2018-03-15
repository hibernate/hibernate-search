/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.bridge.mapping.MarkerBuilder;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoModelCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.SearchException;


/**
 * @author Yoann Rodiere
 */
public class PojoModelNestedElement extends AbstractPojoModelElement
		implements PojoModelProperty, PojoModelCollectorPropertyNode {

	private final AbstractPojoModelElement parent;

	private final PojoPropertyModel<?> propertyModel;

	private final Map<Class<?>, List<?>> markers = new HashMap<>();

	PojoModelNestedElement(AbstractPojoModelElement parent, PojoPropertyModel<?> propertyModel,
			TypeMetadataContributorProvider<PojoTypeMetadataContributor> modelContributorProvider) {
		super( modelContributorProvider );
		this.parent = parent;
		this.propertyModel = propertyModel;
	}

	@Override
	public <T> PojoModelElementAccessor<T> createAccessor(Class<T> requestedType) {
		if ( !isAssignableTo( requestedType ) ) {
			throw new SearchException( "Requested incompatible type for '" + this.createAccessor() + "': '" + requestedType + "'" );
		}
		return new PojoModelPropertyElementAccessor<>( parent.createAccessor(), getHandle() );
	}

	@Override
	public PojoModelElementAccessor<?> createAccessor() {
		return new PojoModelPropertyElementAccessor<>( parent.createAccessor(), getHandle() );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <M> Stream<M> markers(Class<M> markerType) {
		return ( (List<M>) this.markers.getOrDefault( markerType, Collections.emptyList() ) )
				.stream();
	}

	@Override
	public final void marker(MarkerBuilder builder) {
		doAddMarker( builder.build() );
	}

	public PropertyHandle getHandle() {
		return propertyModel.getHandle();
	}

	@Override
	public PojoTypeModel<?> getTypeModel() {
		return propertyModel.getTypeModel();
	}

	@Override
	public String getName() {
		return propertyModel.getName();
	}

	@SuppressWarnings("unchecked")
	private <M> void doAddMarker(M marker) {
		Class<M> markerType = (Class<M>) (
				marker instanceof Annotation ? ((Annotation) marker).annotationType()
				: marker.getClass()
		);
		List<M> list = (List<M>) markers.computeIfAbsent( markerType, ignored -> new ArrayList<M>() );
		list.add( marker );
	}
}

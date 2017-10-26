/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.model.spi.IndexableModel;
import org.hibernate.search.engine.mapper.model.spi.IndexableReference;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.spi.TypeModel;
import org.hibernate.search.util.SearchException;


/**
 * @author Yoann Rodiere
 */
public class PojoRootIndexableModel extends PojoIndexableModel implements IndexableModel {

	private final TypeModel<?> typeModel;

	public PojoRootIndexableModel(TypeModel<?> typeModel,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> modelContributorProvider) {
		super( modelContributorProvider );
		this.typeModel = typeModel;
	}

	@Override
	public <T> IndexableReference<T> asReference(Class<T> requestedType) {
		if ( !isAssignableTo( requestedType ) ) {
			throw new SearchException( "Requested incompatible type for '" + asReference() + "': '" + requestedType + "'" );
		}
		return new PojoRootIndexableReference<>( requestedType );
	}

	@Override
	public PojoIndexableReference<?> asReference() {
		return new PojoRootIndexableReference<>( getJavaType() );
	}

	@Override
	public <M extends Annotation> Stream<M> markers(Class<M> markerType) {
		return Stream.empty();
	}

	@Override
	protected Class<?> getJavaType() {
		return typeModel.getJavaType();
	}

	@Override
	public TypeModel<?> getTypeModel() {
		return typeModel;
	}
}

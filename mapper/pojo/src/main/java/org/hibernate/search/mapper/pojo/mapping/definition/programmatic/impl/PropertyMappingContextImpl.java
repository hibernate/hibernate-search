/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.bridge.mapping.BridgeDefinition;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMappingContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoPropertyNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyDocumentIdMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyIndexedEmbeddedMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingContext;

/**
 * @author Yoann Rodiere
 */
public class PropertyMappingContextImpl
		implements PropertyMappingContext, TypeMappingContributor<PojoTypeNodeMappingCollector> {

	private final TypeMappingContext parent;
	private final String name;

	private final List<TypeMappingContributor<? super PojoPropertyNodeMappingCollector>> children = new ArrayList<>();

	public PropertyMappingContextImpl(TypeMappingContext parent, String name) {
		this.parent = parent;
		this.name = name;
	}

	@Override
	public void contribute(PojoTypeNodeMappingCollector collector) {
		PojoPropertyNodeMappingCollector propertyNodeCollector = collector.property( name );
		children.forEach( child -> child.contribute( propertyNodeCollector ) );
	}

	@Override
	public PropertyDocumentIdMappingContext documentId() {
		PropertyDocumentIdMappingContextImpl child = new PropertyDocumentIdMappingContextImpl( this );
		children.add( child );
		return child;
	}

	@Override
	public PropertyMappingContext property(String propertyName) {
		return parent.property( propertyName );
	}

	@Override
	public PropertyMappingContext bridge(BridgeDefinition<?> definition) {
		children.add( new BridgeMappingContributor( definition ) );
		return this;
	}

	@Override
	public PropertyFieldMappingContext field() {
		PropertyFieldMappingContextImpl child = new PropertyFieldMappingContextImpl( this );
		children.add( child );
		return child;
	}

	@Override
	public PropertyIndexedEmbeddedMappingContext indexedEmbedded() {
		PropertyIndexedEmbeddedMappingContextImpl child = new PropertyIndexedEmbeddedMappingContextImpl( this );
		children.add( child );
		return child;
	}

	@Override
	public PropertyMappingContext containedIn() {
		children.add( c -> c.containedIn() );
		return this;
	}

}

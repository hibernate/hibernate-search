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
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMappingCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMappingContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperImplementor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingContext;
import org.hibernate.search.mapper.pojo.model.impl.PojoIndexedTypeIdentifier;

/**
 * @author Yoann Rodiere
 */
public class TypeMappingContextImpl implements TypeMappingContext, MappingContributor<PojoTypeNodeMappingCollector> {

	private final PojoMapperImplementor mappingType;
	private final Class<?> type;

	private String indexName;
	private final List<TypeMappingContributor<? super PojoTypeNodeMappingCollector>> children = new ArrayList<>();

	public TypeMappingContextImpl(PojoMapperImplementor mappingType, Class<?> type) {
		this.mappingType = mappingType;
		this.type = type;
	}

	@Override
	public void contribute(TypeMappingCollector collector) {
		collector.collect( mappingType, new PojoIndexedTypeIdentifier( type ), indexName, children );
	}

	@Override
	public TypeMappingContext indexed() {
		return indexed( type.getName() );
	}

	@Override
	public TypeMappingContext indexed(String indexName) {
		this.indexName = indexName;
		return this;
	}

	@Override
	public TypeMappingContext bridge(BridgeDefinition<?> definition) {
		children.add( new BridgeMappingContributor( definition ) );
		return this;
	}

	@Override
	public PropertyMappingContext property(String propertyName) {
		PropertyMappingContextImpl child = new PropertyMappingContextImpl( this, propertyName );
		children.add( child );
		return child;
	}

}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.mapper.pojo.bridge.spi.IdentifierBridge;
import org.hibernate.search.engine.common.spi.BeanReference;
import org.hibernate.search.engine.common.spi.ImmutableBeanReference;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoPropertyNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoPropertyNodeModelCollector;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyDocumentIdMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;


/**
 * @author Yoann Rodiere
 */
public class PropertyDocumentIdMappingContextImpl extends DelegatingPropertyMappingContext
		implements PropertyDocumentIdMappingContext,
				PojoNodeMetadataContributor<PojoPropertyNodeModelCollector, PojoPropertyNodeMappingCollector> {

	private BeanReference<IdentifierBridge<?>> bridgeReference;

	public PropertyDocumentIdMappingContextImpl(PropertyMappingContext parent) {
		super( parent );
	}

	@Override
	public void contributeModel(PojoPropertyNodeModelCollector collector) {
		// Nothing to do
	}

	@Override
	public void contributeMapping(PojoPropertyNodeMappingCollector collector) {
		collector.identifierBridge( bridgeReference );
	}

	@Override
	public PropertyDocumentIdMappingContext identifierBridge(String bridgeName) {
		this.bridgeReference = new ImmutableBeanReference<>( bridgeName );
		return this;
	}

	@Override
	public PropertyDocumentIdMappingContext identifierBridge(Class<? extends IdentifierBridge<?>> bridgeClass) {
		this.bridgeReference = new ImmutableBeanReference<>( bridgeClass );
		return this;
	}

	@Override
	public PropertyDocumentIdMappingContext identifierBridge(String bridgeName, Class<? extends IdentifierBridge<?>> bridgeClass) {
		this.bridgeReference = new ImmutableBeanReference<>( bridgeName, bridgeClass );
		return this;
	}

}

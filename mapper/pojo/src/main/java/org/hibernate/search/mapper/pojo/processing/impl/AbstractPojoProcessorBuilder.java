/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.search.engine.bridge.mapping.BridgeDefinition;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingIndexModelCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.model.spi.IndexableModel;
import org.hibernate.search.mapper.pojo.mapping.building.impl.IdentifierMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.impl.PojoRootIndexableModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;
import org.hibernate.search.engine.mapper.processing.spi.ValueProcessor;

/**
 * @author Yoann Rodiere
 */
abstract class AbstractPojoProcessorBuilder implements PojoNodeMappingCollector {

	protected final Class<?> javaType;

	protected final PojoIntrospector introspector;
	protected final TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider;

	protected final IndexableModel indexableModel;
	protected final MappingIndexModelCollector indexModelCollector;

	protected final IdentifierMappingCollector identifierBridgeCollector;

	protected final Collection<ValueProcessor> processors = new ArrayList<>();

	public AbstractPojoProcessorBuilder(
			Class<?> javaType, PojoIntrospector introspector,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider,
			MappingIndexModelCollector indexModelCollector,
			IdentifierMappingCollector identifierBridgeCollector) {
		this.introspector = introspector;
		this.contributorProvider = contributorProvider;

		this.javaType = javaType;
		// XXX do something more with the indexable model, to be able to use it in containedIn processing in particular
		this.indexableModel = new PojoRootIndexableModel( introspector, javaType );
		this.indexModelCollector = indexModelCollector;

		this.identifierBridgeCollector = identifierBridgeCollector;
	}

	@Override
	public void bridge(BridgeDefinition<?> definition) {
		processors.add( indexModelCollector.addBridge( indexableModel, definition ) );
	}

}

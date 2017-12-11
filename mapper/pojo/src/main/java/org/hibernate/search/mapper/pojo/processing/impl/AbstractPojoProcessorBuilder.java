/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeDefinition;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoIndexModelBinder;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.impl.PojoIndexableModel;
import org.hibernate.search.mapper.pojo.model.impl.PojoRootIndexableModel;
import org.hibernate.search.mapper.pojo.model.spi.TypeModel;

/**
 * @author Yoann Rodiere
 */
abstract class AbstractPojoProcessorBuilder implements PojoNodeMappingCollector {

	protected final TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider;

	protected final PojoIndexableModel indexableModel;
	protected final PojoIndexModelBinder indexModelBinder;
	protected final IndexModelBindingContext bindingContext;

	protected final PojoTypeNodeIdentityMappingCollector identityMappingCollector;

	protected final Collection<ValueProcessor> processors = new ArrayList<>();

	public AbstractPojoProcessorBuilder(
			TypeModel<?> typeModel,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider,
			PojoIndexModelBinder indexModelBinder, IndexModelBindingContext bindingContext,
			PojoTypeNodeIdentityMappingCollector identityMappingCollector) {
		this.contributorProvider = contributorProvider;

		// FIXME do something more with the indexable model, to be able to use it in containedIn processing in particular
		this.indexableModel = new PojoRootIndexableModel( typeModel, contributorProvider );
		this.indexModelBinder = indexModelBinder;
		this.bindingContext = bindingContext;

		this.identityMappingCollector = identityMappingCollector;
	}

	@Override
	public void bridge(BridgeDefinition<?> definition) {
		processors.add( indexModelBinder.addBridge( bindingContext, indexableModel, definition ) );
	}

}

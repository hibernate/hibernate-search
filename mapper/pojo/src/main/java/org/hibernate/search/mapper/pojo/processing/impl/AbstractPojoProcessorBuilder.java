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
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.bridge.Bridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoIndexModelBinder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelRootElement;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

/**
 * @author Yoann Rodiere
 */
abstract class AbstractPojoProcessorBuilder implements PojoNodeMappingCollector {

	protected final TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider;

	protected final AbstractPojoProcessorBuilder parent;
	protected final PojoTypeModel<?> typeModel;
	protected final PojoModelRootElement indexableModel;
	protected final PojoIndexModelBinder indexModelBinder;
	protected final IndexModelBindingContext bindingContext;

	protected final PojoTypeNodeIdentityMappingCollector identityMappingCollector;

	protected final Collection<ValueProcessor> processors = new ArrayList<>();

	public AbstractPojoProcessorBuilder(
			AbstractPojoProcessorBuilder parent, PojoTypeModel<?> typeModel,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider,
			PojoIndexModelBinder indexModelBinder, IndexModelBindingContext bindingContext,
			PojoTypeNodeIdentityMappingCollector identityMappingCollector) {
		this.contributorProvider = contributorProvider;

		this.parent = parent;
		this.typeModel = typeModel;
		// FIXME do something more with the indexable model, to be able to use it in containedIn processing in particular
		this.indexableModel = new PojoModelRootElement( typeModel, contributorProvider );
		this.indexModelBinder = indexModelBinder;
		this.bindingContext = bindingContext;

		this.identityMappingCollector = identityMappingCollector;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder( getClass().getSimpleName() )
				.append( "[" );
		appendPath( builder);
		builder.append( "]" );
		return builder.toString();
	}

	@Override
	public void bridge(BridgeBuilder<? extends Bridge> builder) {
		processors.add( indexModelBinder.addBridge( bindingContext, indexableModel, builder ) );
	}

	private void appendPath(StringBuilder builder) {
		if ( parent == null ) {
			appendSelfPath( builder );
		}
		else {
			parent.appendPath( builder );
			builder.append( " => " );
			appendSelfPath( builder );
		}
	}

	protected abstract void appendSelfPath(StringBuilder builder);

}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.bridge.spi.FunctionBridge;
import org.hibernate.search.engine.bridge.spi.IdentifierBridge;
import org.hibernate.search.engine.common.spi.BeanReference;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingIndexModelCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.mapping.building.impl.IdentifierMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoPropertyNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.engine.mapper.processing.spi.ValueProcessor;

/**
 * @author Yoann Rodiere
 */
public class PojoPropertyNodeProcessorBuilder extends AbstractPojoProcessorBuilder
		implements PojoPropertyNodeMappingCollector {

	private final PropertyHandle handle;

	private final Collection<PojoTypeNodeProcessorBuilder> indexedEmbeddedProcessorBuilders = new ArrayList<>();

	public PojoPropertyNodeProcessorBuilder(
			PropertyHandle handle, PojoIntrospector introspector,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider,
			MappingIndexModelCollector indexModelCollector,
			IdentifierMappingCollector identifierMappingCollector) {
		super( handle.getType(), introspector, contributorProvider, indexModelCollector,
				identifierMappingCollector );
		this.handle = handle;
	}

	@Override
	public void functionBridge(BeanReference<? extends FunctionBridge<?, ?>> reference,
			String fieldName, FieldModelContributor fieldModelContributor) {
		String defaultedFieldName = fieldName;
		if ( defaultedFieldName == null ) {
			defaultedFieldName = handle.getName();
		}

		ValueProcessor processor = indexModelCollector.addFunctionBridge(
				indexableModel, javaType, reference, defaultedFieldName, fieldModelContributor );
		processors.add( processor );
	}

	@Override
	public void identifierBridge(BeanReference<IdentifierBridge<?>> converterReference) {
		IdentifierBridge<?> bridge = indexModelCollector.createIdentifierBridge( javaType, converterReference );
		identifierBridgeCollector.collect( handle, bridge );
	}

	@Override
	public void containedIn() {
		// FIXME implement ContainedIn
		// FIXME also bind containedIns to indexedEmbeddeds using the parent's metadata here, if possible?
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public void indexedEmbedded(String relativePrefix, int maxDepth, Set<String> pathFilters) {
		// TODO handle collections

		String defaultedRelativePrefix = relativePrefix;
		if ( defaultedRelativePrefix == null ) {
			defaultedRelativePrefix = handle.getName() + ".";
		}

		PojoIndexedTypeIdentifier typeId = new PojoIndexedTypeIdentifier( javaType );

		Optional<MappingIndexModelCollector> nestedCollectorOptional = indexModelCollector.addIndexedEmbeddedIfIncluded(
				typeId, defaultedRelativePrefix, maxDepth, pathFilters );
		nestedCollectorOptional.ifPresent( nestedCollector -> {
			PojoTypeNodeProcessorBuilder nestedProcessorBuilder = new PojoTypeNodeProcessorBuilder(
					javaType, introspector, contributorProvider, nestedCollector,
					IdentifierMappingCollector.noOp() // Do NOT propagate the ID collector to IndexedEmbeddeds
					);
			indexedEmbeddedProcessorBuilders.add( nestedProcessorBuilder );
			contributorProvider.get( typeId ).forEach( c -> c.contributeMapping( nestedProcessorBuilder ) );
		} );
	}

	public PojoPropertyNodeProcessor build() {
		return new PojoPropertyNodeProcessor( handle, processors, indexedEmbeddedProcessorBuilders );
	}

}

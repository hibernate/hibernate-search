/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorPropertyNode;

public final class ErrorCollectingPojoPropertyMetadataContributor implements PojoPropertyMetadataContributor {

	private List<PojoPropertyMetadataContributor> children;

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorPropertyNode collector) {
		if ( hasContent() ) {
			for ( PojoPropertyMetadataContributor child : children ) {
				child.contributeAdditionalMetadata( collector );
			}
		}
	}

	@Override
	public void contributeIndexMapping(PojoIndexMappingCollectorPropertyNode collector) {
		if ( hasContent() ) {
			for ( PojoPropertyMetadataContributor child : children ) {
				try {
					child.contributeIndexMapping( collector );
				}
				catch (RuntimeException e) {
					collector.failureCollector().add( e );
				}
			}
		}
	}

	public ErrorCollectingPojoPropertyMetadataContributor add(PojoPropertyMetadataContributor child) {
		initChildren();
		this.children.add( child );
		return this;
	}

	public boolean hasContent() {
		return children != null && !children.isEmpty();
	}

	private void initChildren() {
		if ( this.children == null ) {
			this.children = new ArrayList<>();
		}
	}
}

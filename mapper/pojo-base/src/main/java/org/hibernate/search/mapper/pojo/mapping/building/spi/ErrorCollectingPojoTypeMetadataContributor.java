/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;

public final class ErrorCollectingPojoTypeMetadataContributor implements PojoTypeMetadataContributor {

	private List<PojoTypeMetadataContributor> children;

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		if ( children != null ) {
			for ( PojoTypeMetadataContributor child : children ) {
				child.contributeAdditionalMetadata( collector );
			}
		}
	}

	@Override
	public void contributeIndexMapping(PojoIndexMappingCollectorTypeNode collector) {
		if ( children != null ) {
			for ( PojoTypeMetadataContributor child : children ) {
				try {
					child.contributeIndexMapping( collector );
				}
				catch (RuntimeException e) {
					collector.failureCollector().add( e );
				}
			}
		}
	}

	public ErrorCollectingPojoTypeMetadataContributor addAll(Collection<? extends PojoTypeMetadataContributor> children) {
		initChildren();
		this.children.addAll( children );
		return this;
	}

	public ErrorCollectingPojoTypeMetadataContributor add(PojoTypeMetadataContributor child) {
		initChildren();
		this.children.add( child );
		return this;
	}

	private void initChildren() {
		if ( this.children == null ) {
			this.children = new ArrayList<>();
		}
	}
}

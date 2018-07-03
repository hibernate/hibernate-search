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

public final class DelegatingPojoTypeMetadataContributor implements PojoTypeMetadataContributor {

	private List<PojoTypeMetadataContributor> children;

	@Override
	public void contributeModel(PojoAdditionalMetadataCollectorTypeNode collector) {
		if ( children != null ) {
			for ( PojoTypeMetadataContributor child : children ) {
				child.contributeModel( collector );
			}
		}
	}

	@Override
	public void contributeMapping(PojoMappingCollectorTypeNode collector) {
		if ( children != null ) {
			for ( PojoTypeMetadataContributor child : children ) {
				child.contributeMapping( collector );
			}
		}
	}

	public DelegatingPojoTypeMetadataContributor addAll(Collection<? extends PojoTypeMetadataContributor> children) {
		initChildren();
		this.children.addAll( children );
		return this;
	}

	public DelegatingPojoTypeMetadataContributor add(PojoTypeMetadataContributor child) {
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

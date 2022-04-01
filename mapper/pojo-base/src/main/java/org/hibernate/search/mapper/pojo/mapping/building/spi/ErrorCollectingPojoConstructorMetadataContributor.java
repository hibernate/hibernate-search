/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import java.util.ArrayList;
import java.util.List;

public final class ErrorCollectingPojoConstructorMetadataContributor implements PojoConstructorMetadataContributor {

	private List<PojoConstructorMetadataContributor> children;

	@Override
	public void contributeSearchMapping(PojoSearchMappingCollectorConstructorNode collector) {
		if ( hasContent() ) {
			for ( PojoConstructorMetadataContributor child : children ) {
				try {
					child.contributeSearchMapping( collector );
				}
				catch (RuntimeException e) {
					collector.failureCollector().add( e );
				}
			}
		}
	}

	public ErrorCollectingPojoConstructorMetadataContributor add(PojoConstructorMetadataContributor child) {
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

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path;

import java.util.Objects;

import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;

public final class PojoModelPathPropertyNode extends PojoModelPath {

	private final PojoModelPathValueNode parent;
	private final String propertyName;

	PojoModelPathPropertyNode(PojoModelPathValueNode parent, String propertyName) {
		this.parent = parent;
		this.propertyName = propertyName;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		PojoModelPathPropertyNode that = (PojoModelPathPropertyNode) o;
		return Objects.equals( parent, that.parent ) && Objects.equals( propertyName, that.propertyName );
	}

	@Override
	public int hashCode() {
		return Objects.hash( parent, propertyName );
	}

	@Override
	public PojoModelPathValueNode getParent() {
		return parent;
	}

	public PojoModelPathValueNode value(ContainerValueExtractorPath extractorPath) {
		return new PojoModelPathValueNode( this, extractorPath );
	}

	public String getPropertyName() {
		return propertyName;
	}

	@Override
	void appendSelfPath(StringBuilder builder) {
		builder.append( "." ).append( propertyName );
	}
}

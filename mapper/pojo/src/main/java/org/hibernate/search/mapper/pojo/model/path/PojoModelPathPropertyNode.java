/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path;

import java.util.Objects;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorPath;
import org.hibernate.search.util.common.impl.Contracts;

/**
 * A node in a {@link PojoModelPath} representing a property.
 * <p>
 * Property nodes represent the property with no indication as to how its value(s) are extracted:
 * it just represent how to access the property itself.
 * To access the value(s) of that property, additional information is required,
 * and that information is provided by a {@link PojoModelPathValueNode}.
 */
public final class PojoModelPathPropertyNode extends PojoModelPath {

	private final PojoModelPathValueNode parent;
	private final String propertyName;

	PojoModelPathPropertyNode(PojoModelPathValueNode parent, String propertyName) {
		this.parent = parent;
		Contracts.assertNotNullNorEmpty( propertyName, "propertyName" );
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

	public PojoModelPathValueNode value(ContainerExtractorPath extractorPath) {
		return new PojoModelPathValueNode( this, extractorPath );
	}

	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * @return a simple string representation of this path taking into account property nodes only,
	 * in the form "propertyA.propertyB.propertyC".
	 * <p>
	 * Completely ignores container value extractors.
	 */
	public String toPropertyString() {
		StringBuilder builder = new StringBuilder();
		addPropertyPathsRecursively( builder, this );
		return builder.toString();
	}

	@Override
	void appendSelfPath(StringBuilder builder) {
		builder.append( "." ).append( propertyName );
	}

	private static void addPropertyPathsRecursively(StringBuilder builder, PojoModelPathPropertyNode propertyNode) {
		PojoModelPathValueNode parentValueNode = propertyNode.getParent();
		if ( parentValueNode != null ) {
			addPropertyPathsRecursively( builder, parentValueNode.getParent() );
			builder.append( '.' );
		}
		builder.append( propertyNode.getPropertyName() );
	}
}

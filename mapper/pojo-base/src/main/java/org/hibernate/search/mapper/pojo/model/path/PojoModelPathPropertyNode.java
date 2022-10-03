/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A node in a {@link PojoModelPath} representing a property.
 * <p>
 * Property nodes represent the property with no indication as to how its value(s) are extracted:
 * it just represent how to access the property itself.
 * To access the value(s) of that property, additional information is required,
 * and that information is provided by a {@link PojoModelPathValueNode}.
 */
public final class PojoModelPathPropertyNode extends PojoModelPath {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoModelPathValueNode parent;
	private final String propertyName;

	PojoModelPathPropertyNode(PojoModelPathValueNode parent, String propertyName) {
		Contracts.assertNotNullNorEmpty( propertyName, "propertyName" );
		if ( DOT_PATTERN.matcher( propertyName ).find() ) {
			throw log.propertyNameCannotContainDots( propertyName );
		}

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

	/**
	 * @return The model path to the value from which the property represented by this node is extracted.
	 * May be {@code null}.
	 */
	@Override
	public PojoModelPathValueNode parent() {
		return parent;
	}

	/**
	 * @param extractorPath The extractor path allowing extraction of a value from this property.
	 * The extractor path may be invalid: no check will be performed.
	 * @return A new path representing the current path, with an additional access to the given property at the end.
	 */
	public PojoModelPathValueNode value(ContainerExtractorPath extractorPath) {
		return new PojoModelPathValueNode( this, extractorPath );
	}

	/**
	 * @return The name of this property.
	 */
	public String propertyName() {
		return propertyName;
	}

	/**
	 * @return A simple string representation of this path taking into account property nodes only,
	 * in the form {@code propertyA.propertyB.propertyC}.
	 * <p>
	 * Completely ignores container extractors.
	 */
	public String toPropertyString() {
		StringBuilder builder = new StringBuilder();
		addPropertyPathsRecursively( builder, this );
		return builder.toString();
	}

	public Optional<PojoModelPathPropertyNode> relativize(PojoModelPathValueNode other) {
		if ( parent == null ) {
			return Optional.empty();
		}
		else if ( other.equals( parent ) ) {
			return Optional.of( new PojoModelPathPropertyNode( null, propertyName ) );
		}
		else {
			return parent.relativize( other )
					.map( newParent -> new PojoModelPathPropertyNode( newParent, propertyName ) );
		}
	}

	@Override
	void appendSelfPath(StringBuilder builder) {
		builder.append( "." ).append( propertyName );
	}

	private static void addPropertyPathsRecursively(StringBuilder builder, PojoModelPathPropertyNode propertyNode) {
		PojoModelPathValueNode parentValueNode = propertyNode.parent();
		if ( parentValueNode != null ) {
			addPropertyPathsRecursively( builder, parentValueNode.parent() );
			builder.append( '.' );
		}
		builder.append( propertyNode.propertyName() );
	}
}

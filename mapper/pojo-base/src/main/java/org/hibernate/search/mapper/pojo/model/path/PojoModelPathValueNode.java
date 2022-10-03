/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path;

import java.util.Objects;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.util.common.impl.Contracts;

/**
 * A node in a {@link PojoModelPath} representing the value(s) of a property.
 * <p>
 * Value node give information as to how to extract values from a property,
 * by specifying a {@link ContainerExtractorPath}.
 * That path will tell Hibernate Search whether it should
 * it just represent how to access the property itself.
 * To access the value(s) of that property, additional information is required,
 * and that information is provided by a {@link PojoModelPathValueNode}.
 */
public final class PojoModelPathValueNode extends PojoModelPath {

	private final PojoModelPathPropertyNode parent;
	private final ContainerExtractorPath extractorPath;

	PojoModelPathValueNode(PojoModelPathPropertyNode parent, ContainerExtractorPath extractorPath) {
		Contracts.assertNotNull( parent, "parent" );
		Contracts.assertNotNull( extractorPath, "extractorPath" );
		this.parent = parent;
		this.extractorPath = extractorPath;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		PojoModelPathValueNode that = (PojoModelPathValueNode) o;
		return parent.equals( that.parent ) && Objects.equals( extractorPath, that.extractorPath );
	}

	@Override
	public int hashCode() {
		return Objects.hash( parent, extractorPath );
	}

	/**
	 * @return The model path to the property from which the value represented by this node is extracted.
	 */
	@Override
	public PojoModelPathPropertyNode parent() {
		return parent;
	}

	/**
	 * @return The extractor path from the parent property to this value.
	 * The path is guaranteed to be explicit (i.e. it won't be {@link ContainerExtractorPath#defaultExtractors()}).
	 */
	public ContainerExtractorPath extractorPath() {
		return extractorPath;
	}

	/**
	 * @param propertyName The name of a property exposed by the type of this value.
	 * The property name may be invalid: no check will be performed.
	 * @return A new path representing the current path, with an additional access to the given property at the end.
	 */
	public PojoModelPathPropertyNode property(String propertyName) {
		return new PojoModelPathPropertyNode( this, propertyName );
	}

	public Optional<PojoModelPathValueNode> relativize(PojoModelPathValueNode other) {
		return parent.relativize( other )
				.map( newParent -> new PojoModelPathValueNode( newParent, extractorPath ) );
	}

	@Override
	void appendSelfPath(StringBuilder builder) {
		builder.append( extractorPath() );
	}
}

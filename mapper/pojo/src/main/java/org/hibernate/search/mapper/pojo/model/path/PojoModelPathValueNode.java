/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path;

import java.util.Objects;

import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;

public final class PojoModelPathValueNode extends PojoModelPath {

	private final PojoModelPathPropertyNode parent;
	private final ContainerValueExtractorPath extractorPath;

	PojoModelPathValueNode(PojoModelPathPropertyNode parent, ContainerValueExtractorPath extractorPath) {
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
	 * The path is guaranteed to be explicit (i.e. it won't be {@link ContainerValueExtractorPath#defaultExtractors()}).
	 */
	public ContainerValueExtractorPath getExtractorPath() {
		return extractorPath;
	}

	public PojoModelPathPropertyNode property(String propertyName) {
		return new PojoModelPathPropertyNode( this, propertyName );
	}

	@Override
	void appendSelfPath(StringBuilder builder) {
		builder.append( getExtractorPath() );
	}
}

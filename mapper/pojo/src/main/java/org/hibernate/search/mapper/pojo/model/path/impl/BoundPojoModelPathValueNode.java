/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

/**
 * @param <T> The property holder type of this node, i.e. the type from which the property is retrieved.
 * @param <P> The property type of this node, i.e. the type of the property from which the values are extracted.
 * @param <V> The value type of this node, i.e. the type of values extracted from the property.
 */
public class BoundPojoModelPathValueNode<T, P, V> extends BoundPojoModelPath {

	private final BoundPojoModelPathPropertyNode<T, P> parent;
	private final BoundContainerValueExtractorPath<P, V> boundExtractorPath;
	private BoundPojoModelPathOriginalTypeNode<V> elementTypePathNode;

	BoundPojoModelPathValueNode(BoundPojoModelPathPropertyNode<T, P> parent,
			BoundContainerValueExtractorPath<P, V> boundExtractorPath) {
		this.parent = parent;
		this.boundExtractorPath = boundExtractorPath;
	}

	/**
	 * @return The model path to the property from which the value represented by this node is extracted.
	 */
	@Override
	public BoundPojoModelPathPropertyNode<T, P> parent() {
		return parent;
	}

	/**
	 * @return A child path node representing the type of values represented by this node.
	 */
	public BoundPojoModelPathOriginalTypeNode<V> type() {
		if ( elementTypePathNode == null ) {
			elementTypePathNode = new BoundPojoModelPathOriginalTypeNode<>(
					this, boundExtractorPath.getExtractedType()
			);
		}
		return elementTypePathNode;
	}

	/**
	 * @return A child path node representing values represented by this node, casted to the given type.
	 */
	public <U> BoundPojoModelPathCastedTypeNode<V, U> castedType(PojoRawTypeModel<U> typeModel) {
		return new BoundPojoModelPathCastedTypeNode<>( this, typeModel );
	}

	/**
	 * @return The bound extractor path from the parent property to this value.
	 */
	public BoundContainerValueExtractorPath<P, V> getBoundExtractorPath() {
		return boundExtractorPath;
	}

	/**
	 * @return The extractor path from the parent property to this value.
	 * The path is guaranteed to be explicit (i.e. it won't be {@link ContainerValueExtractorPath#defaultExtractors()}).
	 */
	public ContainerValueExtractorPath getExtractorPath() {
		return boundExtractorPath.getExtractorPath();
	}

	public PojoModelPathValueNode toUnboundPath() {
		return parent.toUnboundPath().value( getExtractorPath() );
	}

	@Override
	void appendSelfPath(StringBuilder builder) {
		builder.append( getExtractorPath() );
	}
}

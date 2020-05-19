/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.binding.impl;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

public final class PojoModelPathBinder {

	private PojoModelPathBinder() {
		// Only static methods
	}

	public static <T, P, V> V bind(T typeNode, PojoModelPathValueNode unboundModelPath, PojoModelPathWalker<T, P, V> walker) {
		return applyPath( typeNode, unboundModelPath, walker );
	}

	private static <T, P, V> V applyPath(T rootNode, PojoModelPathValueNode unboundPathValueNode,
			PojoModelPathWalker<T, P, V> walker) {
		P propertyNode = applyPath( rootNode, unboundPathValueNode.parent(), walker );
		ContainerExtractorPath extractorPath = unboundPathValueNode.extractorPath();
		return walker.value( propertyNode, extractorPath );
	}

	private static <T, P, V> P applyPath(T rootNode, PojoModelPathPropertyNode unboundPathPropertyNode,
			PojoModelPathWalker<T, P, V> walker) {
		PojoModelPathValueNode unboundPathParentNode = unboundPathPropertyNode.parent();
		T typeNode;
		if ( unboundPathParentNode != null ) {
			V valueNode = applyPath( rootNode, unboundPathParentNode, walker );
			typeNode = walker.type( valueNode );
		}
		else {
			typeNode = rootNode;
		}
		String propertyName = unboundPathPropertyNode.propertyName();
		return walker.property( typeNode, propertyName );
	}
}

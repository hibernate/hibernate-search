/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.spi;

import org.hibernate.search.mapper.pojo.model.path.PojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

public final class PojoModelPathBinder {

	private PojoModelPathBinder() {
		// Only static methods
	}

	public static <T, P, V> V bind(T rootNode, PojoModelPathValueNode unboundModelPath, PojoModelPathWalker<Void, T, P, V> walker) {
		return bind( null, rootNode, unboundModelPath, walker );
	}

	public static <C, T, P, V> V bind(C context, T rootNode, PojoModelPathValueNode unboundModelPath,
			PojoModelPathWalker<C, T, P, V> walker) {
		return applyPath( context, rootNode, unboundModelPath, walker );
	}

	private static <C, T, P, V> V applyPath(C context, T rootNode, PojoModelPathValueNode unboundPathValueNode,
			PojoModelPathWalker<C, T, P, V> walker) {
		P propertyNode = applyPath( context, rootNode, unboundPathValueNode.parent(), walker );
		return walker.value( context, propertyNode, unboundPathValueNode );
	}

	private static <C, T, P, V> P applyPath(C context, T rootNode, PojoModelPathPropertyNode unboundPathPropertyNode,
			PojoModelPathWalker<C, T, P, V> walker) {
		PojoModelPathValueNode unboundPathParentNode = unboundPathPropertyNode.parent();
		T typeNode;
		if ( unboundPathParentNode != null ) {
			V valueNode = applyPath( context, rootNode, unboundPathParentNode, walker );
			typeNode = walker.type( context, valueNode );
		}
		else {
			typeNode = rootNode;
		}
		return walker.property( context, typeNode, unboundPathPropertyNode );
	}
}

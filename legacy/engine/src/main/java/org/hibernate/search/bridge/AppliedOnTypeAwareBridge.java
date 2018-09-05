/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge;

/**
 * Allows bridges to report the type on which the bridge is applied on.
 *
 * The reported type depends on the type of bridge:
 * <ul>
 * <li>for field bridges the type of the property</li>
 * <li>for class bridges the class itself </li>
 * </ul>
 * is returned.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public interface AppliedOnTypeAwareBridge {

	/**
	 * Set the return type of the bridge (the type of the field linked to the bridge).
	 *
	 * @param returnType return type
	 */
	void setAppliedOnType(Class<?> returnType);
}

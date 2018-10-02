/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean;

/**
 * @author Yoann Rodiere
 */
public interface BeanReference {

	/**
	 * @return The name of the referenced bean.
	 * {@code null} implies no name reference.
	 */
	String getName();

	/**
	 * @return The type of the referenced bean.
	 * {@code null} implies no type reference.
	 */
	Class<?> getType();

}

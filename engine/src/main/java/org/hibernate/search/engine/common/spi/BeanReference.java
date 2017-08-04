/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.spi;

/**
 * @author Yoann Rodiere
 */
public interface BeanReference<T> {

	/**
	 * @return The name of the referenced bean.
	 * {@code null} implies no naming constraint.
	 */
	String getName();

	/**
	 * @return The type of the referenced bean.
	 * {@code null} implies no type constraint.
	 */
	Class<? extends T> getType();

}

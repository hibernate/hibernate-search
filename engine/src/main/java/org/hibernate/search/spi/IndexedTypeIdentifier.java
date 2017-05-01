/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spi;

/**
 * Identifies and indexed type. In applications based on Hibernate ORM this will essentially
 * encapsulate a {@code Class} instance. In upcoming extensions an identifier might be backed
 * by a String.
 * N.B. the Class case is simultaneously considered "legacy" and a special case:
 * an annotated class has direct access to its metadata, while a typical identifier would need
 * to be used to lookup the metadata related to it from an external source.
 *
 * @author Sanne Grinovero (C) 2014 Red Hat Inc.
 */
public interface IndexedTypeIdentifier {

	/**
	 * Each indexed type must be identified by name which is unique
	 * within the scope of a {@link SearchIntegrator}.
	 * @return the name of this type.
	 */
	String getName();

	/**
	 * Return the class type of the unrelying POJO.
	 * @deprecated This only exists to facilitate an iterative integration, and will be removed ASAP.
	 * @return the class of the indexed POJO, or null if not using POJO mapping.
	 */
	@Deprecated
	Class<?> getPojoType();

	/**
	 * @return a representation of this type as a singleton IndexedTypesSet
	 */
	IndexedTypeSet asTypeSet();

}

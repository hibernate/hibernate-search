/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

/**
 * @author Hardy Ferentschik
 */
public interface MetadataProvider {

	/**
	 * Returns the Search related metadata for the specified type.
	 *
	 * @param clazz The type of interest.
	 *
	 * @return the {@code TypeMetadata} for the specified type
	 */
	TypeMetadata getTypeMetadataFor(Class<?> clazz);

	boolean containsSearchMetadata(Class<?> clazz);
}

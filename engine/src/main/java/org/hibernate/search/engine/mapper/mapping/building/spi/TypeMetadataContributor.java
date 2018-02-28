/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

/**
 * @author Yoann Rodiere
 */
public interface TypeMetadataContributor {

	/**
	 * A hook for plugging in custom behavior before metadata are contributed to a nested type.
	 * <p>
	 * Allows to discover metadata lazily during bootstrap, which can be helpful when resolving metadata
	 * from the type itself (Java annotations on a Java type, in particular).
	 *
	 * @param typeModel The type model which is about to be contributed to.
	 */
	void beforeNestedContributions(MappableTypeModel typeModel);

}

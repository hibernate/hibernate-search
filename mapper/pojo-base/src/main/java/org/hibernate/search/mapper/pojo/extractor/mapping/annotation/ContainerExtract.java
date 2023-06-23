/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.mapping.annotation;

/**
 * Control how values are extracted from a POJO property of container type.
 */
public enum ContainerExtract {
	/**
	 * If {@link ContainerExtraction#value() extractors} are defined explicitly, apply those.
	 * Otherwise, automatically and recursively resolve extractors according to the type of the property:
	 * get elements for collections, values for maps, ...
	 */
	DEFAULT,
	/**
	 * Do not apply any container extractor,
	 * and throw an exception if {@link ContainerExtraction#value() extractors} are defined explicitly.
	 */
	NO
}

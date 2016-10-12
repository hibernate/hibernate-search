/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import java.util.List;

import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;

/**
 * Interface implemented by Lucene fields denoting the parent element of
 * the subsequent fields.
 *
 * <p>This is a temporary workaround.
 *
 * @see ElasticsearchNestingContextFactoryProvider
 *
 * @author Yoann Rodiere
 */
public interface NestingMarker {

	List<NestingPathComponent> getPath();

	interface NestingPathComponent {

		/**
		 * @return The type metadata for this path component, providing access
		 * among others to the property name and to the field prefix.
		 */
		EmbeddedTypeMetadata getEmbeddedTypeMetadata();

		/**
		 * @return When the property is a container (array, collection, map),
		 * the index of the selected element. Otherwise, {@code null}.
		 */
		Integer getIndex();

	}

}

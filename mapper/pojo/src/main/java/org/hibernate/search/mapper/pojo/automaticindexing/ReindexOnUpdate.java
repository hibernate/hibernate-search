/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

/**
 * Defines the various strategies for handling updates to a value in an entity.
 * <p>
 * A "value" here means either an entity property or something extracted from that property
 * using a {@link ContainerExtractor}.
 */
public enum ReindexOnUpdate {

	/**
	 * Apply the default, safe behavior:
	 * <ul>
	 *     <li>If a parent value was assigned {@link #NO}, ignore an updates to this value.</li>
	 *     <li>
	 *         Otherwise, track updates to this value, or any nested value,
	 *         but only if they are used in the indexing process.
	 *         Whenever an update is detected, trigger reindexing of the entity holding the updated value,
	 *         and of any other entity accessing this value through an {@link IndexedEmbedded} for example.
	 *      </li>
	 * </ul>
	 * <p>
	 * Note that updates to nested values that were assigned {@link #NO} will always be ignored.
	 */
	DEFAULT,
	/**
	 * Ignore updates to the values (or to any nested value)
	 * and as a consequence never trigger reindexing,
	 * unless an indexed entity is deleted or another, sibling value configured with {@link #DEFAULT} is updated.
	 * <p>
	 * This generally means indexing should be triggered externally to periodically refresh the index.
	 */
	NO

}

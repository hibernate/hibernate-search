/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;

/**
 * Defines the impact that an update to a value in an entity will have on reindexing of this entity and containing entities.
 * <p>
 * A "value" here means either an entity property or something extracted from that property
 * using a {@link ContainerExtractor}.
 */
public enum ReindexOnUpdate {

	/**
	 * Default behavior: updates to the targeted value will trigger automatic reindexing
	 * if it's actually used in the indexing process of an indexed entity,
	 * unless a property on the path from the indexed entity to the targeted value
	 * prevents it through a different {@link ReindexOnUpdate} setting.
	 * <p>
	 * To be precise:
	 * <ul>
	 *     <li>If an indexed entity accesses the targeted value indirectly (e.g. through an {@code @IndexedEmbedded}),
	 *     and a property in the path from the indexed entity to the targeted value was assigned {@link #NO},
	 *     then updates to the targeted value <strong>will not</strong> automatically trigger reindexing of the indexed entity.</li>
	 *     <li>If an indexed entity {@code A} accesses the targeted value indirectly (e.g. through an {@code @IndexedEmbedded}),
	 *     and a property in the path from the indexed entity to the targeted value was assigned {@link #SHALLOW},
	 *     and the targeted value is owned by a different entity {@code B},
	 *     then updates to the targeted value <strong>will not</strong> automatically trigger reindexing of the indexed entity {@code A}.</li>
	 *     <li>Otherwise, assuming the targeted value is actually used in the indexing process of an indexed entity,
	 *     either directly (e.g. {@code @GenericField}) or indirectly (e.g. through {@code @IndexedEmbedded}),
	 *     then updates to the targeted value <strong>will</strong> automatically trigger reindexing of the indexed entity.</li>
	 * </ul>
	 */
	DEFAULT,
	/**
	 * Updates to the targeted value will trigger automatic reindexing
	 * if it's actually used in the indexing process of an indexed entity,
	 * unless a property on the path from the indexed entity to the targeted value
	 * prevents it through a different {@link ReindexOnUpdate} setting.
	 * <p>
	 * However, updates to "nested" values (values that are accessed through the targeted value)
	 * will only trigger automatic reindexing if the "nested" value is owned by the same entity as the targeted value.
	 * When the path from the targeted value to the "nested" values crosses entity boundaries,
	 * the reindex-on-update behavior automatically switches to {@link #NO}.
	 * <p>
	 * Applications relying on this setting should have periodic batch processes in place
	 * to refresh the index of affected entities in case "nested" values changed.
	 */
	SHALLOW,
	/**
	 * Updates to the targeted value, or to any "nested" value (values that are accessed through the targeted value),
	 * will never trigger automatic reindexing.
	 * <p>
	 * Applications relying on this setting should have periodic batch processes in place
	 * to refresh the index of affected entities in case the targeted value changed.
	 */
	NO

}

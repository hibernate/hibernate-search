/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.cfg;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.impl.StringHelper;

/**
 * Strategy for creating/deleting the indexes in Elasticsearch upon Hibernate Search initialization and shut-down.
 *
 * @author Gunnar Morling
 */
public enum ElasticsearchIndexLifecycleStrategyName {

	/**
	 * Indexes will never be created or deleted.
	 * <p>Hibernate Search will not even check that the index actually exists.
	 * <p>The index schema (mapping and analyzer definitions) is not managed by Hibernate Search and is not checked.
	 */
	NONE("none"),

	/**
	 * Upon session factory initialization, existing index mappings will be checked
	 * by Hibernate Search, causing an exception if a required mapping does not exist
	 * or exists but differs in a non-compatible way (more strict type constraints, for instance),
	 * or if an analyzer definition is missing or differs in any way.
	 * <p>This strategy will not bring any change to the mappings or analyzer definitions, nor create or delete any index.
	 */
	VALIDATE("validate"),

	/**
	 * Upon session factory initialization, index mappings and analyzer definitions will be updated to match expected ones,
	 * causing an exception if a mapping to be updated is not compatible with the expected one.
	 * <p>Missing indexes will be created along with their mappings and analyzer definitions.
	 * Missing mappings and analyzer definitions on existing indexes will be created.
	 */
	UPDATE("update"),

	/**
	 * Existing indexes will not be altered, missing indexes will be created along with their mappings and analyzer definitions.
	 */
	CREATE("create"),

	/**
	 * Indexes - and all their contents - will be deleted and newly created (along with their mappings and analyzer definitions)
	 * upon session factory initialization.
	 *
	 * <p>Note that whenever a search factory is altered after initialization (i.e. new entities are mapped),
	 * the index will <strong>not</strong> be deleted again: new mappings will simply be added to the index.
	 */
	DROP_AND_CREATE("drop-and-create"),

	/**
	 * The same as {@link #DROP_AND_CREATE}, but indexes - and all their contents - will be deleted upon session factory
	 * shut-down as well.
	 */
	DROP_AND_CREATE_AND_DROP("drop-and-create-and-drop");

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static ElasticsearchIndexLifecycleStrategyName of(String value) {
		return StringHelper.parseDiscreteValues(
				ElasticsearchIndexLifecycleStrategyName.values(),
				ElasticsearchIndexLifecycleStrategyName::getExternalRepresentation,
				log::invalidIndexLifecycleStrategyName,
				value
		);
	}

	private final String externalRepresentation;

	ElasticsearchIndexLifecycleStrategyName(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	public String getExternalRepresentation() {
		return externalRepresentation;
	}
}

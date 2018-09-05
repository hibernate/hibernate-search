/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.cfg;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Strategy for creating/deleting the indexes in Elasticsearch upon session factory initialization and shut-down.
 *
 * @author Gunnar Morling
 */
public enum IndexSchemaManagementStrategy {

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

	private static final Map<String, IndexSchemaManagementStrategy> VALUES_BY_EXTERNAL_NAME;
	static {
		Map<String, IndexSchemaManagementStrategy> tmpMap = new HashMap<>();
		for ( IndexSchemaManagementStrategy strategy : values() ) {
			tmpMap.put( strategy.externalName.toLowerCase( Locale.ROOT ), strategy );
		}
		VALUES_BY_EXTERNAL_NAME = Collections.unmodifiableMap( tmpMap );
	}

	public static IndexSchemaManagementStrategy interpretPropertyValue(String propertyValue) {
		final String normalizedName = propertyValue.trim().toLowerCase( Locale.ROOT );
		IndexSchemaManagementStrategy strategy = VALUES_BY_EXTERNAL_NAME.get( normalizedName );
		if ( strategy == null ) {
				throw new IllegalArgumentException( "Unrecognized property value for an index schema management strategy: '" + propertyValue
						+ "'. Please use one of " + VALUES_BY_EXTERNAL_NAME.keySet() );
		}
		return strategy;
	}

	private final String externalName;

	private IndexSchemaManagementStrategy(String propertyValue) {
		this.externalName = propertyValue;
	}

	/**
	 * @return the name to use in configuration files.
	 */
	public String getExternalName() {
		return externalName;
	}
}

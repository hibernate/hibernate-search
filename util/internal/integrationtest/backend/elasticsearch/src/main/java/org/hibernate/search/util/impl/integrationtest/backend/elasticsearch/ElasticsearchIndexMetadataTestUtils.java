/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.index.naming.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ElasticsearchIndexMetadataTestUtils {

	private ElasticsearchIndexMetadataTestUtils() {
	}

	public static URLEncodedString defaultPrimaryName(String hibernateSearchIndexName) {
		return IndexNames.encodeName( hibernateSearchIndexName + "-000001" );
	}

	public static URLEncodedString defaultWriteAlias(String hibernateSearchIndexName) {
		return IndexNames.encodeName( hibernateSearchIndexName + "-write" );
	}

	public static URLEncodedString defaultReadAlias(String hibernateSearchIndexName) {
		return IndexNames.encodeName( hibernateSearchIndexName + "-read" );
	}

	public static URLEncodedString encodeName(String name) {
		return IndexNames.encodeName( name );
	}

	public static Set<URLEncodedString> defaultAliases(String hibernateSearchIndexName) {
		return CollectionHelper.asImmutableSet(
				defaultWriteAlias( hibernateSearchIndexName ),
				defaultReadAlias( hibernateSearchIndexName )
		);
	}

	public static JsonObject defaultAliasDefinitions(String hibernateSearchIndexName) {
		return aliasDefinitions(
				defaultWriteAlias( hibernateSearchIndexName ).original,
				defaultReadAlias( hibernateSearchIndexName ).original
		);
	}

	public static JsonObject aliasDefinitions(String writeAlias, String readAlias) {
		JsonObject aliases = new JsonObject();

		if ( writeAlias != null ) {
			aliases.add( writeAlias, writeAliasDefinition() );
		}

		if ( readAlias != null ) {
			aliases.add( readAlias, readAliasDefinition() );
		}

		return aliases;
	}

	public static JsonObject writeAliasDefinition() {
		return aliasDefinition( true, null );
	}

	public static JsonObject readAliasDefinition() {
		return aliasDefinition( false, null );
	}

	public static JsonObject aliasDefinition(boolean isWriteIndex, String otherAttributes) {
		JsonObject result;
		if ( otherAttributes == null || otherAttributes.isEmpty() ) {
			result = new JsonObject();
		}
		else {
			result = new Gson().fromJson( "{" + otherAttributes + "}", JsonObject.class );
		}

		if ( ElasticsearchTestDialect.get().supportsIsWriteIndex() ) {
			result.addProperty( "is_write_index", isWriteIndex );
		}

		return result;
	}

	public static JsonObject mappingWithoutAnyProperty() {
		JsonObject result = new JsonObject();
		result.addProperty( "dynamic", "strict" );
		return result;
	}

	public static JsonObject mappingWithDiscriminatorProperty(String propertyName) {
		JsonObject result = new JsonObject();
		result.addProperty( "dynamic", "strict" );

		JsonObject properties = new JsonObject();
		result.add( "properties", properties );

		properties.add( propertyName, discriminatorMappingComplete() );

		return result;
	}

	public static JsonObject discriminatorMappingComplete() {
		JsonObject discriminatorMapping = new JsonObject();
		discriminatorMapping.addProperty( "type", "keyword" );
		discriminatorMapping.addProperty( "index", false );
		discriminatorMapping.addProperty( "store", false );
		discriminatorMapping.addProperty( "doc_values", true );
		return discriminatorMapping;
	}

	public static JsonObject discriminatorMappingOmitDefaults() {
		JsonObject discriminatorMapping = new JsonObject();
		discriminatorMapping.addProperty( "type", "keyword" );
		discriminatorMapping.addProperty( "index", false );
		// "store" and "doc_values" have default values: omit them.
		return discriminatorMapping;
	}
}

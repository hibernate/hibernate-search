package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.client.common.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.util.common.impl.CollectionHelper;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

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
		return CollectionHelper.asImmutableSet( defaultWriteAlias( hibernateSearchIndexName ),
				defaultReadAlias( hibernateSearchIndexName ) );
	}

	public static ObjectNode defaultAliasDefinitions(String hibernateSearchIndexName) {
		return aliasDefinitions( defaultWriteAlias( hibernateSearchIndexName ).original,
				defaultReadAlias( hibernateSearchIndexName ).original );
	}

	public static ObjectNode aliasDefinitions(String writeAlias, String readAlias) {
		ObjectNode aliases = new ObjectNode( JsonNodeFactory.instance );
		if ( writeAlias != null ) {
			aliases.set( writeAlias, writeAliasDefinition() );
		}
		if ( readAlias != null ) {
			aliases.set( readAlias, readAliasDefinition() );
		}
		return aliases;
	}

	public static ObjectNode writeAliasDefinition() {
		return aliasDefinition( true, null );
	}

	public static ObjectNode readAliasDefinition() {
		return aliasDefinition( false, null );
	}

	public static ObjectNode aliasDefinition(boolean isWriteIndex, String otherAttributes) {
		ObjectNode result;
		if ( otherAttributes == null || otherAttributes.isEmpty() ) {
			result = new ObjectNode( JsonNodeFactory.instance );
		}
		else {
			result = JsonProviderHelper.create( false ).getCodec()
					.decode( "{" + otherAttributes + "}", ObjectNode.class );
		}
		result.put( "is_write_index", isWriteIndex );
		return result;
	}

	public static ObjectNode mappingWithoutAnyProperty() {
		ObjectNode result = new ObjectNode( JsonNodeFactory.instance );
		result.put( "dynamic", "strict" );
		return result;
	}

	public static ObjectNode mappingWithDiscriminatorProperty(String propertyName) {
		ObjectNode result = new ObjectNode( JsonNodeFactory.instance );
		result.put( "dynamic", "strict" );
		ObjectNode properties = new ObjectNode( JsonNodeFactory.instance );
		result.set( "properties", properties );
		properties.set( propertyName, discriminatorMappingComplete() );
		return result;
	}

	public static ObjectNode discriminatorMappingComplete() {
		ObjectNode discriminatorMapping = new ObjectNode( JsonNodeFactory.instance );
		discriminatorMapping.put( "type", "keyword" );
		discriminatorMapping.put( "index", false );
		discriminatorMapping.put( "doc_values", true );
		return discriminatorMapping;
	}

	public static ObjectNode discriminatorMappingOmitDefaults() {
		ObjectNode discriminatorMapping = new ObjectNode( JsonNodeFactory.instance );
		discriminatorMapping.put( "type", "keyword" );
		discriminatorMapping.put( "index", false );
		// "doc_values" has the default value: omit it.
		return discriminatorMapping;
	}
}

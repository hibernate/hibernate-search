package org.hibernate.search.backend.elasticsearch.lowlevel.syntax.metadata.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;

public interface ElasticsearchIndexMetadataSyntax {

	IndexAliasDefinition createWriteAliasDefinition();

	IndexAliasDefinition createReadAliasDefinition();

}

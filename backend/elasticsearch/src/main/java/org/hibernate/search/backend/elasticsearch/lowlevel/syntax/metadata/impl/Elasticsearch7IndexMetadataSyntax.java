/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.syntax.metadata.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;

/**
 * The index metadata syntax for ES7 and later.
 */
public class Elasticsearch7IndexMetadataSyntax implements ElasticsearchIndexMetadataSyntax {

	@Override
	public IndexAliasDefinition createWriteAliasDefinition() {
		IndexAliasDefinition definition = new IndexAliasDefinition();
		definition.setWriteIndex( true );
		return definition;
	}

	@Override
	public IndexAliasDefinition createReadAliasDefinition() {
		IndexAliasDefinition definition = new IndexAliasDefinition();
		definition.setWriteIndex( false );
		return definition;
	}

}

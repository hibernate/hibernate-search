/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

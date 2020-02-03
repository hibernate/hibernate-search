/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.syntax.metadata.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;

/**
 * The index metadata syntax for ES5.6 to 6.3.
 */
public class Elasticsearch56IndexMetadataSyntax implements ElasticsearchIndexMetadataSyntax {

	@Override
	public IndexAliasDefinition createWriteAliasDefinition() {
		// Do not set isWriteIndex: it's not supported in ES 5.6 -> 6.3
		return new IndexAliasDefinition();
	}

	@Override
	public IndexAliasDefinition createReadAliasDefinition() {
		// Do not set isWriteIndex: it's not supported in ES 5.6 -> 6.3
		return new IndexAliasDefinition();
	}

}

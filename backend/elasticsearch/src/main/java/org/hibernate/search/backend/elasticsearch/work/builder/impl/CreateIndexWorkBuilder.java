/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.builder.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.result.impl.CreateIndexResult;


public interface CreateIndexWorkBuilder extends ElasticsearchWorkBuilder<NonBulkableElasticsearchWork<CreateIndexResult>> {

	CreateIndexWorkBuilder ignoreExisting();

	CreateIndexWorkBuilder aliases(Map<String, IndexAliasDefinition> aliases);

	CreateIndexWorkBuilder settings(IndexSettings settings);

	CreateIndexWorkBuilder mapping(RootTypeMapping mapping);
}

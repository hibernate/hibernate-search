/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.builder.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.work.result.impl.CreateIndexResult;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;


public interface CreateIndexWorkBuilder extends ElasticsearchWorkBuilder<ElasticsearchWork<CreateIndexResult>> {

	CreateIndexWorkBuilder ignoreExisting();

	CreateIndexWorkBuilder settings(IndexSettings settings);

	CreateIndexWorkBuilder mapping(RootTypeMapping mapping);
}

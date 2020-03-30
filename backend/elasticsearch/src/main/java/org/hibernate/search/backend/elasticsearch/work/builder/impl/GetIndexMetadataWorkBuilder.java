/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.builder.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;
import org.hibernate.search.backend.elasticsearch.work.result.impl.ExistingIndexMetadata;


public interface GetIndexMetadataWorkBuilder extends ElasticsearchWorkBuilder<NonBulkableWork<List<ExistingIndexMetadata>>> {

	GetIndexMetadataWorkBuilder index(URLEncodedString indexName);

}

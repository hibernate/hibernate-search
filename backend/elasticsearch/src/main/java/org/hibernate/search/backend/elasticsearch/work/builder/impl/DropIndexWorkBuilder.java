/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.builder.impl;

import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableElasticsearchWork;


public interface DropIndexWorkBuilder extends ElasticsearchWorkBuilder<NonBulkableElasticsearchWork<Void>> {

	DropIndexWorkBuilder ignoreIndexNotFound();

}

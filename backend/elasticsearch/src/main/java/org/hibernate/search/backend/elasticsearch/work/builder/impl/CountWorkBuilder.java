/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.builder.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;

import com.google.gson.JsonObject;

public interface CountWorkBuilder extends ElasticsearchWorkBuilder<ElasticsearchWork<Long>> {

	CountWorkBuilder type(URLEncodedString type);

	CountWorkBuilder query(JsonObject query);

	CountWorkBuilder routingKeys(Set<String> routingKeys);

}

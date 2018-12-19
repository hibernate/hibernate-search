/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
// TODO restore the full work factory from Search 5
public interface ElasticsearchWorkFactory {

	ElasticsearchWork<Long> count(Set<URLEncodedString> indexNames, Set<String> routingKeys, JsonObject payload);

}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchStubWorkFactory implements ElasticsearchWorkFactory {

	public ElasticsearchStubWorkFactory() {
	}

	@Override
	public ElasticsearchWork<Long> count(Set<URLEncodedString> indexNames, Set<String> routingKeys, JsonObject payload) {
		ElasticsearchRequest.Builder builder = ElasticsearchRequest.post()
				.multiValuedPathComponent( indexNames )
				.pathComponent( Paths._COUNT )
				.body( payload );

		if ( !routingKeys.isEmpty() ) {
			builder.param( "routing", routingKeys.stream().collect( Collectors.joining( "," ) ) );
		}

		return new ElasticsearchStubWork<>( builder.build(), JsonAccessor.root().property( "count" ).asLong() );
	}
}

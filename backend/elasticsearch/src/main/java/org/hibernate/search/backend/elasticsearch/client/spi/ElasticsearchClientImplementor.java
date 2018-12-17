/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.spi;

import java.io.Closeable;

import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;

/**
 * An interface allowing to configure an {@link ElasticsearchClient} and to close it.
 */
public interface ElasticsearchClientImplementor extends ElasticsearchClient, Closeable {

	void init(GsonProvider gsonProvider);

}

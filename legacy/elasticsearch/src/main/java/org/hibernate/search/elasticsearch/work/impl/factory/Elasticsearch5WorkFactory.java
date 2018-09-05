/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl.factory;

import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.work.impl.ES5DeleteByQueryWork;
import org.hibernate.search.elasticsearch.work.impl.ES5FlushWork;
import org.hibernate.search.elasticsearch.work.impl.ES5OptimizeWork;
import org.hibernate.search.elasticsearch.work.impl.builder.DeleteByQueryWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.FlushWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.OptimizeWorkBuilder;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class Elasticsearch5WorkFactory extends Elasticsearch2WorkFactory {

	public Elasticsearch5WorkFactory(GsonProvider gsonProvider) {
		super( gsonProvider );
	}

	@Override
	public FlushWorkBuilder flush() {
		return new ES5FlushWork.Builder( this );
	}

	@Override
	public OptimizeWorkBuilder optimize() {
		return new ES5OptimizeWork.Builder();
	}

	@Override
	public DeleteByQueryWorkBuilder deleteByQuery(URLEncodedString indexName, JsonObject payload) {
		return new ES5DeleteByQueryWork.Builder( indexName, payload, this );
	}

}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl.factory;

import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;

/**
 * @author Yoann Rodiere
 */
public class Elasticsearch5WorkFactory extends Elasticsearch2WorkFactory {

	public Elasticsearch5WorkFactory(GsonProvider gsonProvider) {
		super( gsonProvider );
	}

}

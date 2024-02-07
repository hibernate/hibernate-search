/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.java.modules.pojo.standalone.lucene.service;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.integrationtest.java.modules.pojo.standalone.lucene.entity.Author;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBinder;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBindingContext;

public final class SimulatedDatastore {
	private SimulatedDatastore() {
	}

	private static final Map<Integer, Author> authors = new HashMap<>();

	public static void clear() {
		authors.clear();
	}

	static void put(Author author) {
		authors.put( author.getId(), author );
	}

	public static class AuthorLoadingBinder implements EntityLoadingBinder {
		@Override
		public void bind(EntityLoadingBindingContext context) {
			context.selectionLoadingStrategy( Author.class, SelectionLoadingStrategy.fromMap( authors ) );
		}
	}
}

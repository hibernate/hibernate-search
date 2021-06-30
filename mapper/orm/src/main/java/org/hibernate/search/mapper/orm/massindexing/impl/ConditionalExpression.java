/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.util.function.Consumer;

import org.hibernate.query.Query;

public class ConditionalExpression {

	private final String hql;

	private Consumer<Query<?>> queryConsumer;

	public ConditionalExpression(String hql) {
		this.hql = hql;
	}

	public String hql() {
		return hql;
	}

	public void defineQueryConsumer(Consumer<Query<?>> queryConsumer) {
		this.queryConsumer = queryConsumer;
	}

	public void applyQueryConsumer(Query<?> query) {
		if ( queryConsumer != null ) {
			queryConsumer.accept( query );
		}
	}
}

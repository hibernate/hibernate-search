/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.spi;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.query.Query;

public final class ConditionalExpression {

	private final String hql;
	private final Map<String, Object> params = new HashMap<>();

	public ConditionalExpression(String hql) {
		this.hql = hql;
	}

	@Override
	public String toString() {
		return "ConditionalExpression[" +
				"hql='" + hql + '\'' +
				", params=" + params +
				']';
	}

	public String hql() {
		return hql;
	}

	public void param(String name, Object value) {
		params.put( name, value );
	}

	public void applyParams(Query<?> query) {
		for ( Map.Entry<String, Object> entry : params.entrySet() ) {
			query.setParameter( entry.getKey(), entry.getValue() );
		}
	}
}

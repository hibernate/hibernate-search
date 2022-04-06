/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect;

import com.google.gson.JsonObject;

public class Elasticsearch5TestDialect extends Elasticsearch60TestDialect {

	@Override
	protected void setLegacyTemplatePattern(JsonObject object, String pattern) {
		object.addProperty( "template", pattern );
	}

	@Override
	public boolean supportsStrictGreaterThanRangedQueriesOnScaledFloatField() {
		return false;
	}

	@Override
	public boolean hasBugForSortMaxOnNegativeFloats() {
		return true;
	}

	@Override
	public boolean supportMoreThan1024Terms() {
		return false;
	}
}

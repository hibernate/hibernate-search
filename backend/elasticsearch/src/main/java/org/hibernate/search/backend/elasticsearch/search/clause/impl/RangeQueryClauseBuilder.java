/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.clause.impl;

/**
 * @author Yoann Rodiere
 */
// TODO move that to engine?
public interface RangeQueryClauseBuilder extends QueryClauseBuilder {

	void lowerLimit(Object value);

	void excludeLowerLimit();

	void upperLimit(Object value);

	void excludeUpperLimit();

}

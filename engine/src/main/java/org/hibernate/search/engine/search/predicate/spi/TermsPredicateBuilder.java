/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.spi;

import java.util.Collection;

import org.hibernate.search.engine.search.common.ValueConvert;

public interface TermsPredicateBuilder extends SearchPredicateBuilder {

	void matchingAny(Collection<?> terms, ValueConvert convert);

	void matchingAll(Collection<?> terms, ValueConvert convert);

	void matchingAnyParam(String parameterName, ValueConvert convert);

	void matchingAllParam(String parameterName, ValueConvert convert);
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.massindexing.util.impl;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * Order over a single ID attribute.
 * <p>
 * This class should be used when target entity has a single ID attribute.
 *
 * @author Mincong Huang
 * @author Yoann Rodiere
 */
public class SingularIdOrder implements IdOrder {

	private final String idPropertyName;

	public SingularIdOrder(String idPropertyName) {
		this.idPropertyName = idPropertyName;
	}

	@Override
	public Predicate idGreater(CriteriaBuilder builder, Root<?> root, Object idObj) {
		return builder.greaterThan( root.get( idPropertyName ), (Comparable<? super Object>) idObj );
	}

	@Override
	public Predicate idGreaterOrEqual(CriteriaBuilder builder, Root<?> root, Object idObj) {
		return builder.greaterThanOrEqualTo( root.get( idPropertyName ), (Comparable<? super Object>) idObj );
	}

	@Override
	public Predicate idLesser(CriteriaBuilder builder, Root<?> root, Object idObj) {
		return builder.lessThan( root.get( idPropertyName ), (Comparable<? super Object>) idObj );
	}

	@Override
	public void addAscOrder(CriteriaBuilder builder, CriteriaQuery<?> criteria, Root<?> root) {
		criteria.orderBy( builder.asc( root.get( idPropertyName ) ) );
	}

}

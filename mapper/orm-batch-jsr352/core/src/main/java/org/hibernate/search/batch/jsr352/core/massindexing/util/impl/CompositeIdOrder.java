/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.massindexing.util.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.IdClass;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.model.domain.NavigableRole;

/**
 * Order over multiple ID attributes.
 * <p>
 * This class should be used when target entity has multiple ID attributes,
 * e.g. an entity annotated {@link IdClass}, or an entity having an
 * {@link EmbeddedId}.
 * <p>
 * By default, {@code Restrictions.ge} and similar on
 * a composite property will generate a non-lexicographical "greater or equal" condition.
 * <p>
 * For instance, a condition such as
 * {@code Restrictions.ge("myEmbeddedDate", MyEmbeddedDate.yearMonthDay( 2017, 6, 20 )}
 * will generate {@code year >= 2017 AND month >= 6 AND day >= 20}
 * which is obviously not what any sane person would expect,
 * since this will exclude July 1st to July 19th (among others).
 * <p>
 * This class fixes the issue by properly implementing lexicographical
 * "greater or equal" and "lesser" conditions.
 *
 * @see <a href="https://hibernate.atlassian.net/browse/HSEARCH-2615">HSEARCH-2615</a>
 *
 * @author Mincong Huang
 * @author Yoann Rodiere
 */
public class CompositeIdOrder implements IdOrder {

	private final EntityIdentifierMapping idMapping;
	private final EmbeddableMappingType idMappingType;

	public CompositeIdOrder(EntityIdentifierMapping idMapping, EmbeddableMappingType idMappingType) {
		this.idMapping = idMapping;
		this.idMappingType = idMappingType;
	}

	@Override
	public Predicate idGreater(CriteriaBuilder builder, Root<?> root, Object idObj) {
		return restrictLexicographically( builder::greaterThan, builder, root, idObj, false );
	}

	@Override
	public Predicate idGreaterOrEqual(CriteriaBuilder builder, Root<?> root, Object idObj) {
		// Caution, using 'builder::greaterThanOrEqualTo' here won't cut it, we really need
		// to separate the strict operator from the equals.
		return restrictLexicographically( builder::greaterThan, builder, root, idObj, true );
	}

	@Override
	public Predicate idLesser(CriteriaBuilder builder, Root<?> root, Object idObj) {
		return restrictLexicographically( builder::lessThan, builder, root, idObj, false );
	}

	@Override
	public void addAscOrder(CriteriaBuilder builder, CriteriaQuery<?> criteria, Root<?> root) {
		ArrayList<Order> orders = new ArrayList<>();
		idMappingType.forEachSubPart( (i, subPart) -> orders.add( builder.asc( toPath( root, subPart ) ) ) );
		criteria.orderBy( orders );
	}

	@SuppressWarnings("unchecked")
	private Predicate restrictLexicographically(
			BiFunction<Expression<Comparable<? super Object>>, Comparable<? super Object>, Predicate> strictOperator,
			CriteriaBuilder builder, Root<?> root, Object idObj, boolean orEquals) {
		Object[] selectableValues = idMappingType.getValues( idObj );
		List<Predicate> or = new ArrayList<>();

		idMappingType.forEachSubPart( (i, subPart) -> {
			// Group expressions together in a single conjunction (A and B and C...).
			Predicate[] and = new Predicate[i + 1];

			idMappingType.forEachSubPart( (j, previousSubPart) -> {
				if ( j < i ) {
					// The first N-1 expressions have symbol `=`
					and[j] = builder.equal( toPath( root, previousSubPart ),
							selectableValues[j] );
				}
			} );
			// The last expression has whatever symbol is defined by "strictOperator"
			and[i] = strictOperator.apply( toPath( root, subPart ),
					(Comparable<? super Object>) selectableValues[i] );

			or.add( builder.and( and ) );
		} );

		if ( orEquals ) {
			Predicate[] and = new Predicate[selectableValues.length];
			idMappingType.forEachSubPart( (i, subPart) -> {
				and[i] = builder.equal( toPath( root, subPart ), selectableValues[i] );
			} );
			or.add( builder.and( and ) );
		}

		// Group the disjunction of multiple expressions (X or Y or Z...).
		return builder.or( or.toArray( new Predicate[0] ) );
	}

	private <T> Path<T> toPath(Root<?> root, ModelPart subPart) {
		return toPath( root, subPart.getNavigableRole() );
	}

	private <T> Path<T> toPath(Path<?> parent, NavigableRole role) {
		if ( role == idMappingType.getNavigableRole() ) {
			return parent.get( idMapping.getAttributeName() );
		}
		else {
			return toPath( parent, role.getParent() ).get( role.getLocalName() );
		}
	}
}

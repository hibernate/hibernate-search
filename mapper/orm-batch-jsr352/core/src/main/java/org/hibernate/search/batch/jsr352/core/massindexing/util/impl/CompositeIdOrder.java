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
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;

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

	private final EntityIdentifierMapping mapping;
	private final EmbeddableMappingType mappingType;

	public CompositeIdOrder(EntityIdentifierMapping mapping, EmbeddableMappingType mappingType) {
		this.mapping = mapping;
		this.mappingType = mappingType;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Predicate idGreater(CriteriaBuilder builder, Root<?> root, Object idObj) {
		BiFunction<String, Object, Predicate> strictOperator = (String path, Object obj) ->
				builder.greaterThan( root.get( path ), (Comparable<? super Object>) idObj );
		return restrictLexicographically( strictOperator, builder, root, idObj, false );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Predicate idGreaterOrEqual(CriteriaBuilder builder, Root<?> root, Object idObj) {
		/*
		 * Caution, using Restrictions::ge here won't cut it, we really need
		 * to separate the strict operator from the equals.
		 */
		BiFunction<String, Object, Predicate> strictOperator = (String path, Object obj) ->
				builder.greaterThan( root.get( path ), (Comparable<? super Object>) idObj );
		return restrictLexicographically( strictOperator, builder, root, idObj, true );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Predicate idLesser(CriteriaBuilder builder, Root<?> root, Object idObj) {
		BiFunction<String, Object, Predicate> strictOperator = (String path, Object obj) ->
				builder.lessThan( root.get( path ), (Comparable<? super Object>) idObj );
		return restrictLexicographically( strictOperator, builder, root, idObj, false );
	}

	@Override
	public void addAscOrder(CriteriaBuilder builder, CriteriaQuery<?> criteria, Root<?> root) {
		ArrayList<Order> orders = new ArrayList<>();
		mapping.forEachSelectable( (i, selectable) ->
				orders.add( builder.asc( root.get( selectable.getSelectablePath().getFullPath() ) ) ) );
		criteria.orderBy( orders );
	}

	private Predicate restrictLexicographically(BiFunction<String, Object, Predicate> strictOperator,
			CriteriaBuilder builder, Root<?> root, Object idObj, boolean orEquals) {
		Object[] selectableValues = mappingType.getValues( idObj );
		int selectablesSize = selectableValues.length;

		List<Predicate> or = new ArrayList<>();

		mapping.forEachSelectable( (i, selectable) -> {
			// Group expressions together in a single conjunction (A and B and C...).
			Predicate[] and = new Predicate[i + 1];

			mapping.forEachSelectable( (j, previousSelectable) -> {
				if ( j < i ) {
					// The first N-1 expressions have symbol `=`
					String path = previousSelectable.getSelectablePath().getFullPath();
					Object val = selectableValues[j];
					and[j] = builder.equal( root.get( path ), val );
				}
			} );
			// The last expression has whatever symbol is defined by "strictOperator"
			String path = selectable.getSelectablePath().getFullPath();
			Object val = selectableValues[i];
			and[i] = strictOperator.apply( path, val );

			or.add( builder.and( and ) );
		} );

		if ( orEquals ) {
			Predicate[] and = new Predicate[selectablesSize];
			mapping.forEachSelectable( (i, previousSelectable) -> {
				String path = previousSelectable.getSelectablePath().getFullPath();
				Object val = selectableValues[i];
				and[i] = builder.equal( root.get( path ), val );
			} );
			or.add( builder.and( and ) );
		}

		// Group the disjunction of multiple expressions (X or Y or Z...).
		return builder.or( or.toArray( new Predicate[0] ) );
	}

}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jakarta.batch.core.massindexing.util.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.IdClass;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.search.mapper.orm.loading.spi.ConditionalExpression;
import org.hibernate.search.mapper.orm.loading.spi.LoadingTypeContext;

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
public class CompositeIdOrder<E> implements IdOrder {

	private final EntityIdentifierMapping idMapping;
	private final EmbeddableMappingType idMappingType;

	public CompositeIdOrder(LoadingTypeContext<E> type) {
		this.idMapping = type.entityMappingType().getIdentifierMapping();
		this.idMappingType = (EmbeddableMappingType) idMapping.getPartMappingType();
	}

	@Override
	public ConditionalExpression idGreater(String paramNamePrefix, Object idObj) {
		return restrictLexicographically( paramNamePrefix, idObj, ">", false );
	}

	@Override
	public ConditionalExpression idGreaterOrEqual(String paramNamePrefix, Object idObj) {
		// Caution, using ">=" here won't cut it, we really need to separate the strict operator from the equals.
		return restrictLexicographically( paramNamePrefix, idObj, ">", true );
	}

	@Override
	public ConditionalExpression idLesser(String paramNamePrefix, Object idObj) {
		return restrictLexicographically( paramNamePrefix, idObj, "<", false );
	}

	@Override
	public String ascOrder() {
		StringBuilder builder = new StringBuilder();
		idMappingType.forEachSubPart( (i, subPart) -> {
			if ( builder.length() != 0 ) {
				builder.append( ", " );
			}
			toPath( builder, subPart.getNavigableRole() );
			builder.append( " asc" );
		} );
		return builder.toString();
	}

	private ConditionalExpression restrictLexicographically(String paramNamePrefix, Object idObj,
			String strictOperator, boolean orEquals) {
		List<String> orClauses = new ArrayList<>();

		idMappingType.forEachSubPart( (i, subPart) -> {
			// Group expressions together in a single conjunction (A and B and C...).
			String[] andClauses = new String[i + 1];

			idMappingType.forEachSubPart( (j, previousSubPart) -> {
				if ( j < i ) {
					// The first N-1 expressions have symbol `=`
					andClauses[j] = toPath( previousSubPart ) + " = :" + paramNamePrefix + j;
				}
			} );

			// The last expression has whatever symbol is defined by "strictOperator"
			andClauses[i] = toPath( subPart ) + " " + strictOperator + " :" + paramNamePrefix + i;

			orClauses.add( junction( Arrays.asList( andClauses ), " and " ) );
		} );


		if ( orEquals ) {
			List<String> andClauses = new ArrayList<>();
			idMappingType.forEachSubPart( (i, subPart) -> andClauses.add( toPath( subPart ) + " = :" + paramNamePrefix + i ) );
			orClauses.add( junction( andClauses, " and " ) );
		}

		var expression = new ConditionalExpression( junction( orClauses, " or " ) );
		Object[] selectableValues = idMappingType.getValues( idObj );
		for ( int i = 0; i < selectableValues.length; i++ ) {
			expression.param( paramNamePrefix + i, selectableValues[i] );
		}
		return expression;
	}

	private String toPath(ModelPart subPart) {
		StringBuilder builder = new StringBuilder();
		toPath( builder, subPart.getNavigableRole() );
		return builder.toString();
	}

	private void toPath(StringBuilder builder, NavigableRole role) {
		if ( role == idMappingType.getNavigableRole() ) {
			builder.append( idMapping.getAttributeName() );
		}
		else {
			toPath( builder, role.getParent() );
			builder.append( "." );
			builder.append( role.getLocalName() );
		}
	}

	private String junction(List<String> clauses, String operator) {
		StringBuilder junctionBuilder = new StringBuilder();
		boolean first = true;
		for ( String clause : clauses ) {
			if ( first ) {
				first = false;
			}
			else {
				junctionBuilder.append( operator );
			}
			junctionBuilder.append( "(" ).append( clause ).append( ")" );
		}
		return junctionBuilder.toString();
	}
}

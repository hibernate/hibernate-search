/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.Query;

import org.hibernate.search.query.dsl.MoreLikeThisTermination;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class ConnectedMoreLikeThisQueryBuilder implements MoreLikeThisTermination {

	private static final Log log = LoggerFactory.make();

	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final FieldsContext fieldsContext;
	private final INPUT_TYPE inputType;
	private final Object input;

	public ConnectedMoreLikeThisQueryBuilder(Object id,
											INPUT_TYPE inputType,
											FieldsContext fieldsContext,
											QueryCustomizer queryCustomizer,
											QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.fieldsContext = fieldsContext;
		this.inputType = inputType;
		this.input = id;
	}

	/**
	 * We could encapsulate that into a MoreLikeThisContentObject but going for this approach for now
	 * to save memory pressure.
	 * If the code becomes too nasty, change it.
	 */
	public static enum INPUT_TYPE {
		ID,
		ENTITY,
		READER,
		STRING
	}

	@Override
	public Query createQuery() {
		return null;
	}

}

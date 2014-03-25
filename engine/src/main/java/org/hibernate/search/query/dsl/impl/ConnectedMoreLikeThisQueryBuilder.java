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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;

import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.dsl.MoreLikeThisTermination;
import org.hibernate.search.query.dsl.MoreLikeThisToEntityContentAndTermination;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public abstract class ConnectedMoreLikeThisQueryBuilder {

	private static final Log log = LoggerFactory.make();

	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final FieldsContext fieldsContext;
	private final INPUT_TYPE inputType;
	private final Object input;
	private final MoreLikeThisQueryContext moreLikeThisContext;

	public ConnectedMoreLikeThisQueryBuilder(Object id,
											INPUT_TYPE inputType,
											FieldsContext fieldsContext,
											MoreLikeThisQueryContext moreLikeThisContext,
											QueryCustomizer queryCustomizer,
											QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.moreLikeThisContext = moreLikeThisContext;
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

	public Query createQuery() {
		Query query;
		final SearchFactoryImplementor searchFactory = queryContext.getFactory();
		final DocumentBuilderIndexedEntity<?> documentBuilder = Helper.getDocumentBuilder( queryContext );
		IndexReader indexReader = searchFactory.getIndexReaderAccessor().open( queryContext.getEntityType() );
		// retrieving the docId and building the more like this query form the term vectors must be using the same index reader
		try {
			String[] fieldNames = getAllCompatibleFieldNames( documentBuilder );
			if ( fieldsContext.size() == 0 ) {
				// Use all compatible fields when comparingAllFields is used
				fieldsContext.addAll( fieldNames );
			}
			query = new MoreLikeThisBuilder( documentBuilder, searchFactory )
					.compatibleFieldNames( fieldNames )
					.fieldsContext( fieldsContext )
					.queryContext( queryContext )
					.indexReader( indexReader )
					.inputType( inputType )
					.input( input )
					.otherMoreLikeThisContext( moreLikeThisContext )
					.createQuery();
		}
		finally {
			searchFactory.getIndexReaderAccessor().close( indexReader );
		}
		//TODO implement INPUT.READER
		//TODO implement INOUT.STRING
		return queryCustomizer.setWrappedQuery( query ).createQuery();
	}

	private String[] getAllCompatibleFieldNames(DocumentBuilderIndexedEntity<?> documentBuilder) {
		Collection<DocumentFieldMetadata> allFieldMetadata = documentBuilder.getTypeMetadata().getAllDocumentFieldMetadata();
		List<String> fieldNames = new ArrayList<String>( allFieldMetadata.size() );
		for ( DocumentFieldMetadata fieldMetadata : allFieldMetadata ) {
			if ( ( fieldMetadata.getTermVector() != Field.TermVector.NO //has term vector
				|| fieldMetadata.getStore() != org.hibernate.search.annotations.Store.NO ) //is stored
				&& ! ( fieldMetadata.isId() || fieldMetadata.isIdInEmbedded() ) ) { //Exclude id fields as they are not meaningful for MoreLikeThis
				fieldNames.add( fieldMetadata.getName() );
			}
		}
		if ( fieldNames.size() == 0 ) {
			throw log.noFieldCompatibleForMoreLikeThis( documentBuilder.getBeanClass() );
		}
		return fieldNames.toArray( new String[fieldNames.size()] );
	}

	public static final class MoreLikeThisTerminationImpl extends ConnectedMoreLikeThisQueryBuilder implements MoreLikeThisTermination {

		public MoreLikeThisTerminationImpl(Object id, INPUT_TYPE inputType, FieldsContext fieldsContext, MoreLikeThisQueryContext moreLikeThisContext, QueryCustomizer queryCustomizer, QueryBuildingContext queryContext) {
			super( id, inputType, fieldsContext, moreLikeThisContext, queryCustomizer, queryContext );
		}
	}

	public static final class MoreLikeThisToEntityContentAndTerminationImpl extends ConnectedMoreLikeThisQueryBuilder implements MoreLikeThisToEntityContentAndTermination {

		public MoreLikeThisToEntityContentAndTerminationImpl(Object id, INPUT_TYPE inputType, FieldsContext fieldsContext, MoreLikeThisQueryContext moreLikeThisContext, QueryCustomizer queryCustomizer, QueryBuildingContext queryContext) {
			super( id, inputType, fieldsContext, moreLikeThisContext, queryCustomizer, queryContext );
		}
	}
}

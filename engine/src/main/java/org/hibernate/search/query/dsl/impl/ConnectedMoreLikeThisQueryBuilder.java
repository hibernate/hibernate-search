/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;

import org.hibernate.search.annotations.Store;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.query.dsl.MoreLikeThisTermination;
import org.hibernate.search.query.dsl.MoreLikeThisToEntityContentAndTermination;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
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
		final ExtendedSearchIntegrator searchIntegrator = queryContext.getFactory();
		final DocumentBuilderIndexedEntity documentBuilder = Helper.getDocumentBuilder( queryContext );
		IndexReader indexReader = searchIntegrator.getIndexReaderAccessor().open( queryContext.getEntityType() );
		// retrieving the docId and building the more like this query form the term vectors must be using the same index reader
		try {
			String[] fieldNames = getAllCompatibleFieldNames( documentBuilder );
			if ( fieldsContext.size() == 0 ) {
				// Use all compatible fields when comparingAllFields is used
				fieldsContext.addAll( fieldNames );
			}
			query = new MoreLikeThisBuilder( documentBuilder, searchIntegrator )
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
			searchIntegrator.getIndexReaderAccessor().close( indexReader );
		}
		//TODO implement INPUT.READER
		//TODO implement INOUT.STRING
		return queryCustomizer.setWrappedQuery( query ).createQuery();
	}

	private String[] getAllCompatibleFieldNames(DocumentBuilderIndexedEntity documentBuilder) {
		Collection<DocumentFieldMetadata> allFieldMetadata = documentBuilder.getTypeMetadata().getAllDocumentFieldMetadata();
		List<String> fieldNames = new ArrayList<String>( allFieldMetadata.size() );
		for ( DocumentFieldMetadata fieldMetadata : allFieldMetadata ) {
			boolean hasTermVector = fieldMetadata.getTermVector() != Field.TermVector.NO;
			boolean isStored = fieldMetadata.getStore() != Store.NO;
			boolean isIdOrEmbeddedId = fieldMetadata.isId() || fieldMetadata.isIdInEmbedded();
			if ( ( hasTermVector || isStored ) && !isIdOrEmbeddedId ) {
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

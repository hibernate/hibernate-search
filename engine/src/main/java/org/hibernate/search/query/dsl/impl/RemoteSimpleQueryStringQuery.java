/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.Query;
import org.hibernate.search.analyzer.impl.RemoteAnalyzerReference;
import org.hibernate.search.exception.AssertionFailure;

/**
 * A query using the {@link SimpleQueryParser} interpreted remotely, thus based on the analyzers defined in the schema.
 *
 * @author Guillaume Smet
 */
public class RemoteSimpleQueryStringQuery extends Query {

	private final String query;

	private final List<Field> fields;

	private final boolean withAndAsDefaultOperator;

	private final RemoteAnalyzerReference originalRemoteAnalyzerReference;

	private final RemoteAnalyzerReference queryRemoteAnalyzerReference;

	private RemoteSimpleQueryStringQuery(String query, List<Field> fields, boolean withAndAsDefaultOperator,
			RemoteAnalyzerReference originalRemoteAnalyzerReference, RemoteAnalyzerReference queryRemoteAnalyzerReference) {
		if ( fields.size() == 0 ) {
			throw new AssertionFailure( "At least one field should be defined for a " + RemoteSimpleQueryStringQuery.class.getSimpleName() );
		}
		this.query = query;
		this.fields = Collections.unmodifiableList( fields );
		this.withAndAsDefaultOperator = withAndAsDefaultOperator;
		this.originalRemoteAnalyzerReference = originalRemoteAnalyzerReference;
		this.queryRemoteAnalyzerReference = queryRemoteAnalyzerReference;
	}

	public static class Builder {
		private String query;

		private List<Field> fields = new ArrayList<Field>();

		private boolean withAndAsDefaultOperator;

		private RemoteAnalyzerReference originalRemoteAnalyzerReference;

		private RemoteAnalyzerReference queryRemoteAnalyzerReference;

		public Builder query(String query) {
			this.query = query;
			return this;
		}

		public Builder field(String name, float boost) {
			this.fields.add( new Field( name, boost ) );
			return this;
		}

		public Builder withAndAsDefaultOperator(boolean withAndAsDefaultOperator) {
			this.withAndAsDefaultOperator = withAndAsDefaultOperator;
			return this;
		}

		public Builder originalRemoteAnalyzerReference(RemoteAnalyzerReference originalRemoteAnalyzerReference) {
			this.originalRemoteAnalyzerReference = originalRemoteAnalyzerReference;
			return this;
		}

		public Builder queryRemoteAnalyzerReference(RemoteAnalyzerReference queryRemoteAnalyzerReference) {
			this.queryRemoteAnalyzerReference = queryRemoteAnalyzerReference;
			return this;
		}

		public RemoteSimpleQueryStringQuery build() {
			return new RemoteSimpleQueryStringQuery( query, fields, withAndAsDefaultOperator, originalRemoteAnalyzerReference, queryRemoteAnalyzerReference );
		}
	}

	public static class Field {

		private final String name;

		private final float boost;

		private Field(String name, float boost) {
			this.name = name;
			this.boost = boost;
		}

		public String getName() {
			return name;
		}

		public float getBoost() {
			return boost;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder( name );
			if ( boost != 1.0f ) {
				sb.append( "^" ).append( boost );
			}
			return sb.toString();
		}
	}

	public String getQuery() {
		return query;
	}

	public List<Field> getFields() {
		return fields;
	}

	public boolean isMatchAll() {
		return withAndAsDefaultOperator;
	}

	public RemoteAnalyzerReference getOriginalRemoteAnalyzerReference() {
		return originalRemoteAnalyzerReference;
	}

	public RemoteAnalyzerReference getQueryRemoteAnalyzerReference() {
		return queryRemoteAnalyzerReference;
	}

	@Override
	public String toString(String field) {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() ).append( "<" );
		sb.append( "query:" ).append( query ).append( ", " );
		sb.append( "fields:" ).append( fields ).append( ", " );
		sb.append( "withAndAsDefaultOperator:" ).append( withAndAsDefaultOperator ).append( ", " );
		sb.append( "originalRemoteAnalyzerReference:" ).append( originalRemoteAnalyzerReference ).append( ", " );
		sb.append( "queryRemoteAnalyzerReference:" ).append( queryRemoteAnalyzerReference );
		sb.append( ">" );

		return sb.toString();
	}
}

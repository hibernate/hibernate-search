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
import org.hibernate.search.exception.AssertionFailure;

/**
 * A query using the {@link SimpleQueryParser} interpreted remotely, thus based on the analyzers defined in the schema.
 *
 * @author Guillaume Smet
 */
public class RemoteSimpleQueryStringQuery extends Query {

	private final String query;

	private final List<Field> fields;

	private final boolean useAndAsDefaultOperator;

	private RemoteSimpleQueryStringQuery(String query, List<Field> fields, boolean useAndAsDefaultOperator) {
		if ( fields.size() == 0 ) {
			throw new AssertionFailure( "At least one field should be defined for a " + RemoteSimpleQueryStringQuery.class.getSimpleName() );
		}
		this.query = query;
		this.fields = Collections.unmodifiableList( fields );
		this.useAndAsDefaultOperator = useAndAsDefaultOperator;
	}

	public static class Builder {
		private String query;

		private List<Field> fields = new ArrayList<Field>();

		private boolean useAndAsDefaultOperator;

		public Builder query(String query) {
			this.query = query;
			return this;
		}

		public Builder field(String name, float boost) {
			this.fields.add( new Field( name, boost ) );
			return this;
		}

		public Builder useAndAsDefaultOperator(boolean useAndAsDefaultOperator) {
			this.useAndAsDefaultOperator = useAndAsDefaultOperator;
			return this;
		}

		public RemoteSimpleQueryStringQuery build() {
			return new RemoteSimpleQueryStringQuery( query, fields, useAndAsDefaultOperator );
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
		return useAndAsDefaultOperator;
	}

	@Override
	public String toString(String field) {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() ).append( "<" );
		sb.append( "query:" ).append( query ).append( ", " );
		sb.append( "fields:" ).append( fields ).append( ", " );
		sb.append( "useAndAsDefaultOperator:" ).append( useAndAsDefaultOperator ).append( ", " );
		sb.append( ">" );

		return sb.toString();
	}

}

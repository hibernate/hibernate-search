/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.NumericUtils;
import org.hibernate.search.backend.DeletionQuery;
import org.hibernate.search.backend.SingularTermQuery;
import org.hibernate.search.backend.SingularTermQuery.Type;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.impl.ScopedAnalyzer;

/**
 * This class has means to convert all (by default) supported DeletionQueries back to Lucene Queries and to their
 * String[] representation and back.
 *
 * @author Martin Braun
 */
public final class DeleteByQuerySupport {

	private DeleteByQuerySupport() {
		throw new AssertionFailure( "can't touch this!" );
	}

	private static final ToLuceneQuery[] TO_LUCENE_QUERY_CONVERTER;
	static {
		{
			Map<Integer, ToLuceneQuery> map = new HashMap<>();

			map.put( SingularTermQuery.QUERY_KEY, new ToLuceneQuery() {

				@Override
				public Query build(DeletionQuery deletionQuery, ScopedAnalyzer analyzerForEntity) {
					SingularTermQuery query = (SingularTermQuery) deletionQuery;
					if ( query.getType() == Type.STRING ) {
						try {
							TokenStream tokenStream = analyzerForEntity.tokenStream( query.getFieldName(), (String) query.getValue() );
							tokenStream.reset();
							try {
								BooleanQuery booleanQuery = new BooleanQuery();
								while ( tokenStream.incrementToken() ) {
									String value = tokenStream.getAttribute( CharTermAttribute.class ).toString();
									booleanQuery.add( new TermQuery( new Term( query.getFieldName(), value ) ), Occur.MUST );
								}
								return booleanQuery;
							}
							finally {
								tokenStream.close();
							}
						}
						catch (IOException e) {
							throw new AssertionFailure( "no IOException can occur while using a TokenStream that is generated via String" );
						}
					}
					else {
						Type type = query.getType();
						BytesRef valueAsBytes;
						switch ( type ) {
							case INT:
							case FLOAT: {
								int value;
								if ( type == Type.FLOAT ) {
									value = NumericUtils.floatToSortableInt( (Float) query.getValue() );
								}
								else {
									value = (Integer) query.getValue();
								}
								BytesRefBuilder builder = new BytesRefBuilder();
								NumericUtils.intToPrefixCoded( value, 0, builder );
								valueAsBytes = builder.get();
								break;
							}
							case LONG:
							case DOUBLE: {
								long value;
								if ( type == Type.DOUBLE ) {
									value = NumericUtils.doubleToSortableLong( (Double) query.getValue() );
								}
								else {
									value = (Long) query.getValue();
								}
								BytesRefBuilder builder = new BytesRefBuilder();
								NumericUtils.longToPrefixCoded( value, 0, builder );
								valueAsBytes = builder.get();
								break;
							}
							default:
								throw new AssertionFailure( "has to be a Numeric Type at this point!" );
						}
						return new TermQuery( new Term( query.getFieldName(), valueAsBytes ) );
					}

				}

			} );

			TO_LUCENE_QUERY_CONVERTER = new ToLuceneQuery[map.size()];
			for ( Map.Entry<Integer, ToLuceneQuery> entry : map.entrySet() ) {
				TO_LUCENE_QUERY_CONVERTER[entry.getKey()] = entry.getValue();
			}
		}
	}

	public static ToLuceneQuery getToLuceneQuery(int queryKey) {
		return TO_LUCENE_QUERY_CONVERTER[queryKey];
	}

	private static final Set<Class<? extends DeletionQuery>> SUPPORTED_TYPES;
	static {
		{
			Map<Integer, Class<? extends DeletionQuery>> map = new HashMap<>();

			map.put( SingularTermQuery.QUERY_KEY, SingularTermQuery.class );

			SUPPORTED_TYPES = Collections.unmodifiableSet( new HashSet<>( map.values() ) );
		}
	}

	public static boolean isSupported(Class<? extends DeletionQuery> type) {
		return SUPPORTED_TYPES.contains( type );
	}

	private static final StringToQueryMapper[] FROM_STRING;
	static {
		{
			Map<Integer, StringToQueryMapper> map = new HashMap<>();

			map.put( SingularTermQuery.QUERY_KEY, new StringToQueryMapper() {

				@Override
				public DeletionQuery fromString(String[] string) {
					if ( string.length != 3 ) {
						throw new IllegalArgumentException( "for a TermQuery to work there have to be " + "exactly 3 Arguments (type & fieldName & value" );
					}
					Type type = Type.valueOf( string[0] );
					switch ( type ) {
						case STRING:
							return new SingularTermQuery( string[1], string[2] );
						case INT:
							return new SingularTermQuery( string[1], Integer.parseInt( string[2] ) );
						case FLOAT:
							return new SingularTermQuery( string[1], Float.parseFloat( string[2] ) );
						case LONG:
							return new SingularTermQuery( string[1], Long.parseLong( string[2] ) );
						case DOUBLE:
							return new SingularTermQuery( string[1], Double.parseDouble( string[2] ) );
						default:
							throw new AssertionFailure( "wrong Type!" );
					}

				}

			} );

			FROM_STRING = new StringToQueryMapper[map.size()];
			for ( Map.Entry<Integer, StringToQueryMapper> entry : map.entrySet() ) {
				FROM_STRING[entry.getKey()] = entry.getValue();
			}
		}
	}

	public static StringToQueryMapper getStringToQueryMapper(int queryKey) {
		return FROM_STRING[queryKey];
	}

	private static final QueryToStringMapper[] TO_STRING;
	static {
		{
			Map<Integer, QueryToStringMapper> map = new HashMap<>();

			map.put( SingularTermQuery.QUERY_KEY, new QueryToStringMapper() {

				@Override
				public String[] toString(DeletionQuery deletionQuery) {
					SingularTermQuery query = (SingularTermQuery) deletionQuery;
					return new String[] { query.getType().toString(), query.getFieldName(), String.valueOf( query.getValue() ) };
				}

			} );

			TO_STRING = new QueryToStringMapper[map.size()];
			for ( Map.Entry<Integer, QueryToStringMapper> entry : map.entrySet() ) {
				TO_STRING[entry.getKey()] = entry.getValue();
			}
		}
	}

	public static QueryToStringMapper getQueryToStringMapper(int queryKey) {
		return TO_STRING[queryKey];
	}

	static {
		// make sure everything is setup correctly
		Set<Integer> counts = new HashSet<>();
		counts.add( TO_LUCENE_QUERY_CONVERTER.length );
		counts.add( TO_STRING.length );
		counts.add( FROM_STRING.length );
		counts.add( SUPPORTED_TYPES.size() );
		if ( counts.size() != 1 ) {
			throw new AssertionFailure( "all Maps/Sets inside this class must have the same "
					+ "size. Make sure that every QueryType is found in every Map/Set" );
		}
	}

	/**
	 * Interface to map the several <code>DeleteByQuery</code>s to a Lucene query. This is done outside of the
	 * <code>DeleteByQuery</code> class to not overcomplicate the serialization process
	 *
	 * @author Martin Braun
	 */
	public interface ToLuceneQuery {

		Query build(DeletionQuery deletionQuery, ScopedAnalyzer analyzerForEntity);

	}

	/*
	 * Why we use String arrays here: Why use Strings at all and not i.e. byte[] ?: As we want to support different
	 * versions of Queries later on and we don't want to write Serialization code over and over again in the
	 * Serialization module for all the different types we have one toString and fromString here. and we force to use
	 * Strings because i.e. using byte[] would suggest using Java Serialization for this process, but that would prevent
	 * us from changing our API internally in the future. Why not plain Strings?: But using plain Strings would leave us
	 * with yet another problem: We would have to encode all our different fields into a single string. For that we
	 * would need some magical chars to separate or we would need to use something like JSON/XML to pass the data
	 * consistently. This would be far too much overkill for us. By using String[] we force users (or API designers) to
	 * no use Java Serialization (well Serialization to BASE64 or another String representation is still possible, but
	 * nvm that) and we still have an _easy_ way of dealing with multiple fields in our different Query Types.
	 */

	public interface StringToQueryMapper {

		DeletionQuery fromString(String[] string);

	}

	public interface QueryToStringMapper {

		String[] toString(DeletionQuery deletionQuery);

	}

}

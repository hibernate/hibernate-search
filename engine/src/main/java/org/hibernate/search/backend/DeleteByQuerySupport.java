/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.util.Version;
import org.hibernate.search.util.impl.ScopedAnalyzer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This class has means to convert all (by default) supported DeletionQueries back to Lucene Queries and to their
 * String[] representation and back.
 *
 * @author Martin Braun
 */
public final class DeleteByQuerySupport {

	private static final Log log = LoggerFactory.make();

	private DeleteByQuerySupport() {
		throw new AssertionError( "can't touch this!" );
	}

	private static final ConcurrentHashMap<String, CustomBehaviour> CUSTOM_BEHAVIOURS = new ConcurrentHashMap<>();

	private static CustomBehaviour customBehaviour(String className) {
		return CUSTOM_BEHAVIOURS.computeIfAbsent( className, new Function<String, CustomBehaviour>() {

			@Override
			public CustomBehaviour apply(String className) {
				try {
					return (CustomBehaviour) Class.forName( className ).newInstance();
				}
				catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
					//TODO: maybe add a nicer exception here or logging
					throw new RuntimeException( e );
				}
			}

		} );
	}

	public static final Map<Integer, ToLuceneQuery> TO_LUCENE_QUERY_CONVERTER;
	static {
		{
			Map<Integer, ToLuceneQuery> map = new HashMap<>();

			map.put( SingularTermQuery.QUERY_KEY, new ToLuceneQuery() {

				@Override
				public Query build(DeletionQuery deletionQuery, ScopedAnalyzer analyzerForEntity) {
					SingularTermQuery query = (SingularTermQuery) deletionQuery;
					try {
						TokenStream tokenStream = analyzerForEntity.tokenStream( query.getFieldName(), query.getValue() );
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
						throw new AssertionError( "no IOException can occur while using a TokenStream " + "that is generated via String" );
					}
				}

			} );

			map.put( ClassicQueryParserQuery.QUERY_KEY, new ToLuceneQuery() {

				@Override
				public Query build(DeletionQuery deletionQuery, ScopedAnalyzer analyzerForEntity) {
					QueryParser queryParser = new QueryParser( "", analyzerForEntity );
					ClassicQueryParserQuery query = (ClassicQueryParserQuery) deletionQuery;
					try {
						return queryParser.parse( query.getQuery() );
					}
					catch (ParseException e) {
						throw new RuntimeException( e );
					}
				}

			} );

			map.put( CustomBehaviourQuery.QUERY_KEY, new ToLuceneQuery() {

				@Override
				public Query build(DeletionQuery deletionQuery, ScopedAnalyzer analyzerForEntity) {
					CustomBehaviourQuery query = (CustomBehaviourQuery) deletionQuery;
					return customBehaviour( query.getBehaviourClass() ).toLuceneQuery( query, analyzerForEntity );
				}
			} );

			TO_LUCENE_QUERY_CONVERTER = Collections.unmodifiableMap( map );
		}
	}

	public static final Map<Integer, Class<? extends DeletionQuery>> SUPPORTED_TYPES;
	static {
		{
			Map<Integer, Class<? extends DeletionQuery>> map = new HashMap<>();

			map.put( SingularTermQuery.QUERY_KEY, SingularTermQuery.class );
			map.put( ClassicQueryParserQuery.QUERY_KEY, ClassicQueryParserQuery.class );
			map.put( CustomBehaviourQuery.QUERY_KEY,  CustomBehaviourQuery.class );

			SUPPORTED_TYPES = Collections.unmodifiableMap( map );
		}
	}

	public static final Map<Integer, StringToQueryMapper> FROM_STRING;
	static {
		{
			Map<Integer, StringToQueryMapper> map = new HashMap<>();

			map.put( SingularTermQuery.QUERY_KEY, new StringToQueryMapper() {

				@Override
				public DeletionQuery fromString(String[] string) {
					if ( string.length != 2 ) {
						throw new IllegalArgumentException( "for a TermQuery to work there have to be " + "exactly 2 Arguments (fieldName & value" );
					}
					return new SingularTermQuery( string[0], string[1] );
				}

			} );

			map.put( ClassicQueryParserQuery.QUERY_KEY, new StringToQueryMapper() {

				@Override
				public DeletionQuery fromString(String[] string) {
					try {
						Version version = Version.parse( string[0] );
						if ( !version.equals( ClassicQueryParserQuery.LUCENE_VERSION ) ) {
							log.warnf( "using ClassicQueryParserQuery for deletion with " + "different version than in use on this instance. "
									+ "this could yield unexpected behaviour." );
						}
						return new ClassicQueryParserQuery( Version.parse( string[0] ), string[1] );
					}
					catch (java.text.ParseException e) {
						// forward compatible Version.parse. should not happen
						throw new RuntimeException( e );
					}
				}

			} );
			
			map.put(CustomBehaviourQuery.QUERY_KEY, new StringToQueryMapper() {
				
				@Override
				public DeletionQuery fromString(String[] string) {
					String behaviourClass = string[0];
					String[] restArray = new String[string.length - 1];
					System.arraycopy( string, 1, restArray, 0, restArray.length );
					return customBehaviour( behaviourClass ).fromString( restArray );
				}
				
			});

			FROM_STRING = Collections.unmodifiableMap( map );
		}
	}

	public static final Map<Integer, QueryToStringMapper> TO_STRING;
	static {
		{
			Map<Integer, QueryToStringMapper> map = new HashMap<>();

			map.put( SingularTermQuery.QUERY_KEY, new QueryToStringMapper() {

				@Override
				public String[] toString(DeletionQuery deletionQuery) {
					SingularTermQuery query = (SingularTermQuery) deletionQuery;
					return new String[] { query.getFieldName(), query.getValue() };
				}

			} );

			map.put( ClassicQueryParserQuery.QUERY_KEY, new QueryToStringMapper() {

				@Override
				public String[] toString(DeletionQuery deletionQuery) {
					ClassicQueryParserQuery query = (ClassicQueryParserQuery) deletionQuery;
					return new String[] { query.getVersion().toString(), query.getQuery() };
				}

			} );
			
			map.put(CustomBehaviourQuery.QUERY_KEY, new QueryToStringMapper() {
				
				@Override
				public String[] toString(DeletionQuery deletionQuery) {
					CustomBehaviourQuery query = (CustomBehaviourQuery) deletionQuery;
					String[] customData = customBehaviour( query.getBehaviourClass() ).toString( query );
					String[] ret = new String[customData.length + 1];
					ret[0] = query.getBehaviourClass();
					System.arraycopy( customData, 0, ret, 1, customData.length );
					return ret;
				}
				
			});

			TO_STRING = Collections.unmodifiableMap( map );
		}
	}

	static {
		// make sure everything is setup correctly
		Set<Integer> counts = new HashSet<>();
		counts.add( TO_LUCENE_QUERY_CONVERTER.size() );
		counts.add( TO_STRING.size() );
		counts.add( FROM_STRING.size() );
		counts.add( SUPPORTED_TYPES.size() );
		if ( counts.size() != 1 ) {
			throw new AssertionError( "all Maps/Sets inside this class must have the same " + "size. Make sure that every QueryType is found in every Map/Set" );
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

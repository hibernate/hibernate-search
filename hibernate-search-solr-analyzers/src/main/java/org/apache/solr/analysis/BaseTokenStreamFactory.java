package org.apache.solr.analysis;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.util.Version;
import org.apache.solr.common.ResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.StrUtils;
import org.apache.solr.util.Constants;


/**
 * Simple abstract implementation that handles init arg processing, is not really
 * a factory as it implements no interface, but removes code duplication
 * in its subclasses.
 *
 * @version $Id: BaseTokenStreamFactory.java 929782 2010-04-01 02:15:27Z rmuir $
 */
abstract class BaseTokenStreamFactory {
	/**
	 * The init args
	 */
	protected Map<String, String> args;

	/**
	 * the luceneVersion arg
	 */
	protected Version luceneMatchVersion = null;

	public void init(Map<String, String> args) {
		this.args = args;
		String matchVersion = args.get( Constants.LUCENE_MATCH_VERSION_PARAM );
		if ( matchVersion != null ) {
			luceneMatchVersion = parseLuceneVersionString( matchVersion );
		}
	}

	public Map<String, String> getArgs() {
		return args;
	}

	/**
	 * this method can be called in the {@link #create} method,
	 * to inform user, that for this factory a {@link #luceneMatchVersion} is required
	 */
	protected final void assureMatchVersion() {
		if ( luceneMatchVersion == null ) {
			throw new RuntimeException(
					"Configuration Error: Factory '" + this.getClass().getName() +
							"' needs a 'luceneMatchVersion' parameter"
			);
		}
	}

	// TODO: move these somewhere that tokenizers and others
	// can also use them...

	protected int getInt(String name) {
		return getInt( name, -1, false );
	}

	protected int getInt(String name, int defaultVal) {
		return getInt( name, defaultVal, true );
	}

	protected int getInt(String name, int defaultVal, boolean useDefault) {
		String s = args.get( name );
		if ( s == null ) {
			if ( useDefault ) {
				return defaultVal;
			}
			throw new RuntimeException( "Configuration Error: missing parameter '" + name + "'" );
		}
		return Integer.parseInt( s );
	}

	protected boolean getBoolean(String name, boolean defaultVal) {
		return getBoolean( name, defaultVal, true );
	}

	protected boolean getBoolean(String name, boolean defaultVal, boolean useDefault) {
		String s = args.get( name );
		if ( s == null ) {
			if ( useDefault ) {
				return defaultVal;
			}
			throw new RuntimeException( "Configuration Error: missing parameter '" + name + "'" );
		}
		return Boolean.parseBoolean( s );
	}

	protected CharArraySet getWordSet(ResourceLoader loader,
									  String wordFiles, boolean ignoreCase) throws IOException {
		assureMatchVersion();
		List<String> files = StrUtils.splitFileNames( wordFiles );
		CharArraySet words = null;
		if ( files.size() > 0 ) {
			// default stopwords list has 35 or so words, but maybe don't make it that
			// big to start
			words = new CharArraySet( files.size() * 10, ignoreCase );
			for ( String file : files ) {
				List<String> wlist = loader.getLines( file.trim() );
				words.addAll(
						StopFilter.makeStopSet(
								wlist,
								ignoreCase
						)
				);
			}
		}
		return words;
	}

	private Version parseLuceneVersionString(final String matchVersion) {
		String parsedMatchVersion = matchVersion.toUpperCase( Locale.ENGLISH );

		// be lenient with the supplied version parameter
		parsedMatchVersion = parsedMatchVersion.replaceFirst( "^(\\d)\\.(\\d)$", "LUCENE_$1$2" );

		final Version version;
		try {
			version = Version.valueOf( parsedMatchVersion );
		}
		catch ( IllegalArgumentException iae ) {
			throw new SolrException(
					SolrException.ErrorCode.SERVER_ERROR,
					"Invalid luceneMatchVersion '" + matchVersion +
							"', valid values are: " + Arrays.toString( Version.values() ) +
							" or a string in format 'V.V'", iae, false
			);
		}

		return version;
	}
}

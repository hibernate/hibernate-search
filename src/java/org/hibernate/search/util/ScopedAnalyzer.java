//$Id$
package org.hibernate.search.util;

import java.io.Reader;
import java.util.Map;
import java.util.HashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

/**
 * @author Emmanuel Bernard
 */
public class ScopedAnalyzer extends Analyzer {
	public ScopedAnalyzer() {}

	private Analyzer globalAnalyzer;
	private Map<String, Analyzer> scopedAnalyzers = new HashMap<String, Analyzer>();

	public void setGlobalAnalyzer(Analyzer globalAnalyzer) {
		this.globalAnalyzer = globalAnalyzer;
	}

	public void addScopedAnalyzer(String scope, Analyzer scopedAnalyzer) {
		scopedAnalyzers.put( scope, scopedAnalyzer );
	}

	public TokenStream tokenStream(String fieldName, Reader reader) {
		return getAnalyzer( fieldName ).tokenStream( fieldName, reader );
	}

	public int getPositionIncrementGap(String fieldName) {
		return getAnalyzer( fieldName ).getPositionIncrementGap( fieldName );
	}

	private Analyzer getAnalyzer(String fieldName) {
		Analyzer analyzer = scopedAnalyzers.get( fieldName );
		if (analyzer == null) {
			analyzer = globalAnalyzer;
		}
		return analyzer;
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetsConfig;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Helper class to manage a {@literal FacetsConfig} instance:
 * this needs to be lazily allocated as it's a quite heavy object.
 * Ideally it should be reused across threads, but since field options
 * are index specific that doesn't suit our current model.
 */
public final class FacetHandling {

	private static final Log log = LoggerFactory.make();

	private FacetsConfig facetConfig = null;

	public Document build(Document doc) {
		if ( facetConfig == null ) {
			return doc;
		}
		else {
			try {
				return facetConfig.build( doc );
			}
			catch (IOException e) {
				throw log.errorDuringFacetingIndexing( e );
			}
		}
	}

	public void setMultiValued(String facetName) {
		facetConfig.setMultiValued( facetName, true );
	}

	public void enableFacetProcessing() {
		if ( facetConfig == null ) {
			facetConfig = new FacetsConfig();
		}
	}

}

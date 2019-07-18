/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.util.HashSet;
import java.util.Set;

public class LuceneDocumentStoredFieldVisitorBuilder {

	private boolean entireDocumentRequired = false;

	private final Set<String> explicitlyRequired = new HashSet<>();
	private final Set<String> nestedDocumentPaths = new HashSet<>();

	public void addEntireDocument() {
		entireDocumentRequired = true;
		explicitlyRequired.clear();
	}

	public void add(String absoluteFieldPath) {
		if ( !entireDocumentRequired ) {
			explicitlyRequired.add( absoluteFieldPath );
		}
	}

	public void addNestedDocumentPaths(Set<String> nestedDocumentPaths) {
		this.nestedDocumentPaths.addAll( nestedDocumentPaths );
	}

	public void add(String absoluteFieldPath, Set<String> nestedDocumentPaths) {
		add( absoluteFieldPath );
		addNestedDocumentPaths( nestedDocumentPaths );
	}

	public ReusableDocumentStoredFieldVisitor build() {
		if ( entireDocumentRequired ) {
			return new ReusableDocumentStoredFieldVisitor();
		}
		else {
			return new ReusableDocumentStoredFieldVisitor( explicitlyRequired, nestedDocumentPaths );
		}
	}

}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class FieldProjectionRequestContext implements ProjectionRequestContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ProjectionRequestRootContext root;
	private final String absoluteCurrentFieldPath;
	private final String[] absoluteCurrentFieldPathComponents;
	private final String[] relativeCurrentFieldPathComponents;

	public FieldProjectionRequestContext(ProjectionRequestRootContext root,
			String absoluteCurrentFieldPath, String[] absoluteCurrentFieldPathComponents) {
		this( root, absoluteCurrentFieldPath, absoluteCurrentFieldPathComponents,
				absoluteCurrentFieldPathComponents
		);
	}

	private FieldProjectionRequestContext(ProjectionRequestRootContext root,
			String absoluteCurrentFieldPath, String[] absoluteCurrentFieldPathComponents,
			String[] relativeCurrentFieldPathComponents) {
		this.root = root;
		this.absoluteCurrentFieldPath = absoluteCurrentFieldPath;
		this.absoluteCurrentFieldPathComponents = absoluteCurrentFieldPathComponents;
		this.relativeCurrentFieldPathComponents = relativeCurrentFieldPathComponents;
	}

	@Override
	public void checkValidField(String absoluteFieldPath) {
		if ( !FieldPaths.isStrictPrefix( absoluteCurrentFieldPath, absoluteFieldPath ) ) {
			throw log.invalidContextForProjectionOnField( absoluteFieldPath, absoluteCurrentFieldPath );
		}
	}

	@Override
	public ProjectionRequestRootContext root() {
		return root;
	}

	@Override
	public ProjectionRequestContext forField(String absoluteFieldPath, String[] absoluteFieldPathComponents) {
		checkValidField( absoluteFieldPath );
		String[] relativeFieldPathComponents = Arrays.copyOfRange( absoluteFieldPathComponents,
				absoluteCurrentFieldPathComponents.length, absoluteFieldPathComponents.length );
		return new FieldProjectionRequestContext( root, absoluteFieldPath, absoluteFieldPathComponents,
				relativeFieldPathComponents );
	}

	@Override
	public String absoluteCurrentFieldPath() {
		return absoluteCurrentFieldPath;
	}

	@Override
	public String[] relativeCurrentFieldPathComponents() {
		return relativeCurrentFieldPathComponents;
	}
}

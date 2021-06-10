/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.spi;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.common.spi.FieldPaths.RelativizedPath;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;


public abstract class AbstractIndexFieldTemplate<
				M extends AbstractIndexModel<?, ?, F>,
				F extends IndexField<?, C>,
				C extends IndexCompositeNode<?, ?, ?>,
				FT
		> {

	private final FT type;
	private final IndexFieldInclusion inclusion;

	private final SimpleGlobPattern absolutePathGlob;
	private final boolean multiValued;

	public AbstractIndexFieldTemplate(C declaringParent, SimpleGlobPattern absolutePathGlob,
			FT type, IndexFieldInclusion inclusion, boolean multiValued) {
		this.absolutePathGlob = absolutePathGlob;
		this.type = type;
		this.inclusion = declaringParent.inclusion().compose( inclusion );
		this.multiValued = multiValued;
	}

	public final FT type() {
		return type;
	}

	public final IndexFieldInclusion inclusion() {
		return inclusion;
	}

	F createNodeIfMatching(M model, C root, String absolutePath) {
		if ( !absolutePathGlob.matches( absolutePath ) ) {
			return null;
		}

		RelativizedPath relativizedPath = FieldPaths.relativize( absolutePath );
		C parent = relativizedPath.parentPath
				// Must use an explicit type argument for the Eclipse compiler (ECJ)
				.<C>map( path -> {
					// Must use a local variable to clarify the type for the Eclipse compiler (ECJ)
					IndexField<?, C> field = model.fieldOrNull( path, IndexFieldFilter.ALL );
					return field.toComposite();
				} )
				.orElse( root );

		return createNode( parent, relativizedPath.relativePath, type, inclusion, multiValued );
	}

	protected abstract F createNode(C parent, String relativePath, FT type, IndexFieldInclusion inclusion, boolean multiValued);
}

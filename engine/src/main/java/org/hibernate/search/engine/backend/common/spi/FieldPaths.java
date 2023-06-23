/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.common.spi;

import java.util.Optional;

import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;

public class FieldPaths {

	public static final char PATH_SEPARATOR = '.';
	public static final String PATH_SEPARATOR_STRING = String.valueOf( PATH_SEPARATOR );
	public static final String PATH_SEPARATOR_REGEX_STRING = "\\.";

	private FieldPaths() {
	}

	public static String prefix(String prefix, String relativeFieldName) {
		if ( prefix == null ) {
			return relativeFieldName;
		}

		return prefix + relativeFieldName;
	}

	public static SimpleGlobPattern prefix(String prefix, SimpleGlobPattern relativeFieldPathGlob) {
		if ( prefix == null ) {
			return relativeFieldPathGlob;
		}

		return relativeFieldPathGlob.prependLiteral( prefix );
	}

	public static String compose(String absolutePath, String relativeFieldName) {
		if ( absolutePath == null ) {
			return relativeFieldName;
		}

		return absolutePath + PATH_SEPARATOR + relativeFieldName;
	}

	public static SimpleGlobPattern compose(String absolutePath, SimpleGlobPattern relativeFieldPathGlob) {
		if ( absolutePath == null ) {
			return relativeFieldPathGlob;
		}

		return relativeFieldPathGlob.prependLiteral( absolutePath + PATH_SEPARATOR );
	}

	public static SimpleGlobPattern absolutize(String absoluteParentPath, String prefix,
			SimpleGlobPattern relativeFieldPathGlob) {
		return compose( absoluteParentPath, prefix( prefix, relativeFieldPathGlob ) );
	}

	public static RelativizedPath relativize(String absolutePath) {
		int lastSeparatorIndex = absolutePath.lastIndexOf( PATH_SEPARATOR );
		if ( lastSeparatorIndex < 0 ) {
			return new RelativizedPath( Optional.empty(), absolutePath );
		}

		return new RelativizedPath(
				Optional.of( absolutePath.substring( 0, lastSeparatorIndex ) ),
				absolutePath.substring( lastSeparatorIndex + 1 )
		);
	}

	public static boolean isStrictPrefix(String prefixCandidatePath, String path) {
		if ( prefixCandidatePath == null ) {
			return !path.isEmpty();
		}
		if ( prefixCandidatePath.length() >= path.length() ) {
			return false;
		}
		return path.startsWith( prefixCandidatePath )
				&& path.charAt( prefixCandidatePath.length() ) == PATH_SEPARATOR;
	}

	public static String[] split(String absoluteFieldPath) {
		return absoluteFieldPath.split( PATH_SEPARATOR_REGEX_STRING );
	}

	public static final class RelativizedPath {
		public final Optional<String> parentPath;
		public final String relativePath;

		private RelativizedPath(Optional<String> parentPath, String relativePath) {
			this.parentPath = parentPath;
			this.relativePath = relativePath;
		}
	}
}

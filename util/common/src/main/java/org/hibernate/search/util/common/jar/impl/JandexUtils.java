/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.jar.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Repeatable;
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.Type;

public final class JandexUtils {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final DotName REPEATABLE = DotName.createSimple( Repeatable.class.getName() );

	private static final String META_INF_VERSIONS = "META-INF/versions/";
	private static final String META_INF_JANDEX_INDEX = "META-INF/jandex.idx";


	private JandexUtils() {
	}

	public static IndexView emptyIndex() {
		// Apparently there is no dedicated type for an empty index?
		// This should work fine, though.
		return CompositeIndex.create();
	}

	public static IndexView compositeIndex(Collection<IndexView> jandexIndexes) {
		switch ( jandexIndexes.size() ) {
			case 0:
				return emptyIndex();
			case 1:
				return jandexIndexes.iterator().next();
			default:
				return CompositeIndex.create( jandexIndexes );
		}
	}

	public static Set<DotName> findAnnotatedAnnotationsAndContaining(IndexView index, DotName metaAnnotation) {
		Set<DotName> annotations = new HashSet<>();
		for ( AnnotationInstance retentionAnnotation : index.getAnnotations( metaAnnotation ) ) {
			ClassInfo annotation = retentionAnnotation.target().asClass();
			annotations.add( annotation.name() );
			AnnotationInstance repeatable = annotation.classAnnotation( REPEATABLE );
			if ( repeatable != null ) {
				Type containing = repeatable.value().asClass();
				annotations.add( containing.name() );
			}
		}
		return annotations;
	}

	public static ClassInfo extractDeclaringClass(AnnotationTarget target) {
		switch ( target.kind() ) {
			case CLASS:
				return target.asClass();
			case FIELD:
				return target.asField().declaringClass();
			case METHOD:
				return target.asMethod().declaringClass();
			case METHOD_PARAMETER:
				return target.asMethodParameter().method().declaringClass();
			case TYPE:
				return extractDeclaringClass( target.asType().enclosingTarget() );
			default:
				throw new AssertionFailure( "Unsupported annotation target kind: " + target.kind() );
		}
	}

	public static Index readOrBuildIndex(Path jarOrDirectoryPath) {
		try ( FileSystem jarFs = JarUtils.openJarOrDirectory( jarOrDirectoryPath ) ) {
			Path jarRoot = jarFs == null ? jarOrDirectoryPath : jarFs.getRootDirectories().iterator().next();
			Optional<Index> readIndex = doReadIndex( jarRoot );
			if ( readIndex.isPresent() ) {
				return readIndex.get();
			}
			try {
				return doBuildJandexIndex( jarRoot );
			}
			catch (IOException | RuntimeException e) {
				throw log.errorBuildingJandexIndex( jarOrDirectoryPath, e.getMessage(), e );
			}
		}
		catch (IOException | URISyntaxException | RuntimeException e) {
			throw log.errorAccessingJandexIndex( jarOrDirectoryPath, e.getMessage(), e );
		}
	}

	public static Optional<Index> readIndex(Path jarOrDirectoryPath) {
		try ( FileSystem jarFs = JarUtils.openJarOrDirectory( jarOrDirectoryPath ) ) {
			Path jarRoot = jarFs == null ? jarOrDirectoryPath : jarFs.getRootDirectories().iterator().next();
			return doReadIndex( jarRoot );
		}
		catch (IOException | URISyntaxException | RuntimeException e) {
			throw log.errorAccessingJandexIndex( jarOrDirectoryPath, e.getMessage(), e );
		}
	}

	private static Optional<Index> doReadIndex(Path jarRoot) throws IOException {
		Path jandexIndexPath = jarRoot.resolve( META_INF_JANDEX_INDEX );
		if ( !Files.exists( jandexIndexPath ) ) {
			return Optional.empty();
		}
		try ( InputStream in = Files.newInputStream( jandexIndexPath ) ) {
			IndexReader reader = new IndexReader( in );
			return Optional.of( reader.read() );
		}
	}

	/**
	 * Code originally released under ASL 2.0.
	 * <p>
	 * Original code: https://github.com/quarkusio/quarkus/blob/8d4d3459b01203d2ce35d7847874a88941960443/core/deployment/src/main/java/io/quarkus/deployment/index/IndexingUtil.java
	 */
	private static Index doBuildJandexIndex(Path jarRoot) throws IOException {
		Indexer indexer = new Indexer();
		boolean multiRelease = JarUtils.isMultiRelease( jarRoot );
		Path metaInfVersions = jarRoot.resolve( META_INF_VERSIONS );
		try ( Stream<Path> stream = Files.walk( jarRoot ) ) {
			for ( Iterator<Path> it = stream.iterator(); it.hasNext(); ) {
				Path path = it.next();
				if ( path.getFileName() == null || !path.getFileName().toString().endsWith( ".class" ) ) {
					continue;
				}
				if ( multiRelease && path.startsWith( metaInfVersions ) ) {
					if ( isUnsupportedVersionPath( metaInfVersions, path ) ) {
						continue;
					}
				}
				try ( InputStream inputStream = Files.newInputStream( path ) ) {
					indexer.index( inputStream );
				}
			}
		}
		return indexer.complete();
	}

	private static boolean isUnsupportedVersionPath(Path metaInfVersions, Path path) {
		Path relative = metaInfVersions.relativize( path );
		if ( relative.getNameCount() < 2 ) {
			log.debug( "Unexpected structure for META-INF/versions entry: " + path );
			return true;
		}
		try {
			int ver = Integer.parseInt( relative.getName( 0 ).toString() );
			if ( ver > JarUtils.javaVersion() ) {
				return true;
			}
		}
		catch (NumberFormatException ex) {
			log.debug( "Failed to parse META-INF/versions entry: " + path, ex );
			return true;
		}
		return false;
	}
}

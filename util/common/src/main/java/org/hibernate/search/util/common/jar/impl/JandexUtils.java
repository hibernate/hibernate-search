/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.jar.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Repeatable;
import java.lang.invoke.MethodHandles;
import java.net.URL;
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
			AnnotationInstance repeatable = annotation.declaredAnnotation( REPEATABLE );
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

	public static Index readOrBuildIndex(URL codeSourceLocation) {
		try ( CodeSource codeSource = new CodeSource( codeSourceLocation ) ) {
			Optional<Index> readIndex = doReadIndex( codeSource );
			if ( readIndex.isPresent() ) {
				return readIndex.get();
			}
			try {
				return doBuildJandexIndex( codeSource.classesPathOrFail() );
			}
			catch (IOException | RuntimeException e) {
				throw log.errorBuildingJandexIndex( codeSourceLocation, e.getMessage(), e );
			}
		}
		catch (IOException | RuntimeException e) {
			throw log.errorAccessingJandexIndex( codeSourceLocation, e.getMessage(), e );
		}
	}

	public static Optional<Index> readIndex(URL codeSourceLocation) {
		try ( CodeSource codeSource = new CodeSource( codeSourceLocation ) ) {
			return doReadIndex( codeSource );
		}
		catch (IOException | RuntimeException e) {
			throw log.errorAccessingJandexIndex( codeSourceLocation, e.getMessage(), e );
		}
	}

	private static Optional<Index> doReadIndex(CodeSource codeSource) throws IOException {
		try ( InputStream in = codeSource.readOrNull( META_INF_JANDEX_INDEX ) ) {
			if ( in == null ) {
				return Optional.empty();
			}
			IndexReader reader = new IndexReader( in );
			return Optional.of( reader.read() );
		}
	}

	/**
	 * Code originally released under ASL 2.0.
	 * <p>
	 * Original code: https://github.com/quarkusio/quarkus/blob/8d4d3459b01203d2ce35d7847874a88941960443/core/deployment/src/main/java/io/quarkus/deployment/index/IndexingUtil.java
	 */
	private static Index doBuildJandexIndex(Path classesPath) throws IOException {
		Indexer indexer = new Indexer();
		boolean multiRelease = JarUtils.isMultiRelease( classesPath );
		Path metaInfVersions = classesPath.resolve( META_INF_VERSIONS );
		try ( Stream<Path> stream = Files.walk( classesPath ) ) {
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

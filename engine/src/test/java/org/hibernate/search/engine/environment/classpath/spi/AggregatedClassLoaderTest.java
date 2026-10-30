package org.hibernate.search.engine.environment.classpath.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ServiceConfigurationError;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AggregatedClassLoaderTest {

	@TempDir
	Path temporaryDirectory;

	@Test
	void brokenServiceDeclarationsStillFail() throws Exception {
		Path declaration = temporaryDirectory.resolve( "META-INF/services/java.lang.Runnable" );
		Files.createDirectories( declaration.getParent() );
		Files.writeString( declaration, "does.not.Exist" );
		try ( URLClassLoader loader = new URLClassLoader(
				new URL[] { temporaryDirectory.toUri().toURL() }, ClassLoader.getPlatformClassLoader() ) ) {
			ServiceResolver resolver = DefaultServiceResolver.create( AggregatedClassLoader.createFrom( loader ) );
			assertThatThrownBy( () -> resolver.loadJavaServices( Runnable.class ) )
					.isInstanceOf( ServiceConfigurationError.class ).hasMessageContaining( "does.not.Exist" );
		}
	}

	@Test
	void explicitLoadersAreCopiedAndDoNotIncludeAmbientLoaders() throws Exception {
		ClassLoader empty = new ClassLoader( null ) {
		};
		ClassLoader[] loaders = { empty, empty };
		AggregatedClassLoader aggregate = AggregatedClassLoader.createFrom( loaders );
		loaders[0] = getClass().getClassLoader();
		assertThatThrownBy( () -> aggregate.loadClass( getClass().getName() ) )
				.isInstanceOf( ClassNotFoundException.class );
		assertThat( aggregate.getResource( getClass().getName().replace( '.', '/' ) + ".class" ) ).isNull();
		assertThat( aggregate.loadClass( String.class.getName() ) ).isEqualTo( String.class );
	}

	@Test
	void explicitLoadersResolveClassesAndResources() throws Exception {
		AggregatedClassLoader aggregate = AggregatedClassLoader.createFrom( getClass().getClassLoader() );
		assertThat( aggregate.loadClass( getClass().getName() ) ).isEqualTo( getClass() );
		assertThat( aggregate.getResource( getClass().getName().replace( '.', '/' ) + ".class" ) ).isNotNull();
		assertThat( AggregatedClassLoader.createDefault().loadClass( getClass().getName() ) ).isEqualTo( getClass() );
	}

	@Test
	void rejectsNullLoaders() {
		assertThatThrownBy( () -> AggregatedClassLoader.createFrom( (ClassLoader[]) null ) )
				.isInstanceOf( NullPointerException.class );
		assertThatThrownBy( () -> AggregatedClassLoader.createFrom( (ClassLoader) null ) )
				.isInstanceOf( NullPointerException.class );
	}
}

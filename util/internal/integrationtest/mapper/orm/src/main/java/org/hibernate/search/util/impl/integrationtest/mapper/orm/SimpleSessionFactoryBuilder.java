/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.TcclLookupPrecedence;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

import org.jboss.logging.Logger;

public final class SimpleSessionFactoryBuilder {
	private static final Logger log = Logger.getLogger( SimpleSessionFactoryBuilder.class.getName() );

	private boolean cleanUpSchemaOnBuildFailure = true;
	private final List<Consumer<BootstrapServiceRegistryBuilder>> bootstrapServiceRegistryBuilderContributors =
			new ArrayList<>();
	private final List<Consumer<StandardServiceRegistryBuilder>> serviceRegistryBuilderContributors = new ArrayList<>();
	private final List<Consumer<MetadataSources>> metadataSourcesContributors = new ArrayList<>();
	private final List<Consumer<MetadataImplementor>> metadataContributors = new ArrayList<>();
	private final List<Consumer<SessionFactoryBuilder>> sessionFactoryBuilderContributors = new ArrayList<>();

	public void setCleanUpSchemaOnBuildFailure(boolean cleanUpSchemaOnBuildFailure) {
		this.cleanUpSchemaOnBuildFailure = cleanUpSchemaOnBuildFailure;
	}

	@SuppressForbiddenApis(reason = "Strangely, this API involves the internal TcclLookupPrecedence class,"
			+ " and there's nothing we can do about it")
	public SimpleSessionFactoryBuilder setTcclLookupPrecedenceBefore() {
		return onBootstrapServiceRegistryBuilder( builder -> builder.applyTcclLookupPrecedence( TcclLookupPrecedence.BEFORE ) );
	}

	public SimpleSessionFactoryBuilder setProperty(String key, Object value) {
		return onServiceRegistryBuilder( builder -> {
			if ( value == null ) {
				log.infof( "Not setting the property with key '%s' because value is null.", key );
			}
			else {
				builder.applySetting( key, value );
			}
		} );
	}

	public SimpleSessionFactoryBuilder addAnnotatedClass(Class<?> clazz) {
		return onMetadataSources( sources -> sources.addAnnotatedClass( clazz ) );
	}

	public SimpleSessionFactoryBuilder addAnnotatedClasses(Class<?> firstClass, Class<?>... otherClasses) {
		return addAnnotatedClasses( CollectionHelper.asList( firstClass, otherClasses ) );
	}

	public SimpleSessionFactoryBuilder addAnnotatedClasses(Iterable<Class<?>> classes) {
		return onMetadataSources( sources -> classes.forEach( sources::addAnnotatedClass ) );
	}

	public SimpleSessionFactoryBuilder addHbmFromClassPath(String firstPath, String... otherPaths) {
		return addHbmFromClassPath( CollectionHelper.asList( firstPath, otherPaths ) );
	}

	public SimpleSessionFactoryBuilder addHbmFromClassPath(Iterable<String> paths) {
		return onMetadataSources( sources -> paths.forEach( sources::addResource ) );
	}

	public SimpleSessionFactoryBuilder onBootstrapServiceRegistryBuilder(
			Consumer<BootstrapServiceRegistryBuilder> contributor) {
		bootstrapServiceRegistryBuilderContributors.add( contributor );
		return this;
	}

	public SimpleSessionFactoryBuilder onServiceRegistryBuilder(Consumer<StandardServiceRegistryBuilder> contributor) {
		serviceRegistryBuilderContributors.add( contributor );
		return this;
	}

	public SimpleSessionFactoryBuilder onMetadataSources(Consumer<MetadataSources> contributor) {
		metadataSourcesContributors.add( contributor );
		return this;
	}

	public SimpleSessionFactoryBuilder onMetadata(Consumer<MetadataImplementor> contributor) {
		metadataContributors.add( contributor );
		return this;
	}

	public SimpleSessionFactoryBuilder onSessionFactoryBuilder(Consumer<SessionFactoryBuilder> contributor) {
		sessionFactoryBuilderContributors.add( contributor );
		return this;
	}

	private void doFirstPhase(BuildState state) {
		BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder = new BootstrapServiceRegistryBuilder();
		bootstrapServiceRegistryBuilderContributors.forEach( c -> c.accept( bootstrapServiceRegistryBuilder ) );
		state.bootstrapServiceRegistry = bootstrapServiceRegistryBuilder.build();

		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder( state.bootstrapServiceRegistry );
		serviceRegistryBuilderContributors.forEach( c -> c.accept( registryBuilder ) );
		state.serviceRegistry = registryBuilder.build();

		MetadataSources metadataSources = new MetadataSources( state.serviceRegistry );
		metadataSourcesContributors.forEach( c -> c.accept( metadataSources ) );
		state.metadata = metadataSources.buildMetadata();

		metadataContributors.forEach( c -> c.accept( (MetadataImplementor) state.metadata ) );
	}

	public SessionFactory build() {
		BuildState state = new BuildState();
		try {
			doFirstPhase( state );

			SessionFactoryBuilder sessionFactoryBuilder = state.metadata.getSessionFactoryBuilder();
			sessionFactoryBuilderContributors.forEach( c -> c.accept( sessionFactoryBuilder ) );

			try {
				return sessionFactoryBuilder.build();
			}
			catch (RuntimeException | AssertionError e) {
				// Schema creation happens on session factory building,
				// so we only need to do this if we fail during session factory building.
				if ( cleanUpSchemaOnBuildFailure ) {
					new SuppressingCloser( e )
							.push( this::cleanUpSchema );
				}
				throw e;
			}
		}
		catch (RuntimeException e) {
			state.closeOnFailure( e );
			throw e;
		}
	}

	// A failure to build the session factory triggers closing the service registry,
	// which pretty much prevents doing *anything*, including dropping the schema.
	// So... we'll need to restart part of the build.
	private void cleanUpSchema() {
		BuildState state = new BuildState();
		try {
			doFirstPhase( state );

			@SuppressWarnings({ "unchecked", "rawtypes" })
			Map<String, Object> settingsSnapshot = new HashMap<>(
					( state.serviceRegistry.getService( ConfigurationService.class ) ).getSettings() );
			settingsSnapshot.put( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "drop" );
			settingsSnapshot.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, "none" );
			SchemaManagementToolCoordinator.process(
					state.metadata,
					state.serviceRegistry,
					settingsSnapshot,
					action -> {
						throw new IllegalStateException( "No delayed action was expected" );
					}
			);
		}
		catch (RuntimeException e) {
			state.closeOnFailure( e );
			throw e;
		}
	}

	static class BuildState {
		BootstrapServiceRegistry bootstrapServiceRegistry;
		ServiceRegistry serviceRegistry;
		Metadata metadata;

		void closeOnFailure(Throwable t) {
			new SuppressingCloser( t )
					.push( bootstrapServiceRegistry )
					.push( serviceRegistry );
		}
	}

}

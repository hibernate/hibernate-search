package org.hibernate.search.backend.elasticsearch.aws.impl;

import java.time.Clock;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName;
import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.aws.cfg.ElasticsearchAwsBackendSettings;
import org.hibernate.search.backend.elasticsearch.aws.cfg.ElasticsearchAwsCredentialsTypeNames;
import org.hibernate.search.backend.elasticsearch.aws.logging.impl.AwsLog;
import org.hibernate.search.backend.elasticsearch.aws.spi.ElasticsearchAwsCredentialsProvider;
import org.hibernate.search.backend.elasticsearch.aws.spi.ElasticsearchAwsRequestSigning;
import org.hibernate.search.backend.elasticsearch.client.common.authentication.spi.ElasticsearchRequestPreparation;
import org.hibernate.search.backend.elasticsearch.client.common.authentication.spi.ElasticsearchRequestPreparationProvider;
import org.hibernate.search.backend.elasticsearch.client.common.authentication.spi.ElasticsearchRequestPreparationProviderContext;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptor;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptorProvider;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptorProviderContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;

public class ElasticsearchAwsSigningInterceptorProvider
		implements ElasticsearchRequestInterceptorProvider, ElasticsearchRequestPreparationProvider {

	private static final ConfigurationProperty<Boolean> SIGNING_ENABLED =
			ConfigurationProperty.forKey( ElasticsearchAwsBackendSettings.SIGNING_ENABLED )
					.asBoolean()
					.withDefault( ElasticsearchAwsBackendSettings.Defaults.SIGNING_ENABLED )
					.build();

	private static final OptionalConfigurationProperty<String> REGION =
			ConfigurationProperty.forKey( ElasticsearchAwsBackendSettings.REGION )
					.asString()
					.build();

	private static final ConfigurationProperty<BeanReference<? extends ElasticsearchAwsCredentialsProvider>> CREDENTIALS_TYPE =
			ConfigurationProperty.forKey( ElasticsearchAwsBackendSettings.CREDENTIALS_TYPE )
					.asBeanReference( ElasticsearchAwsCredentialsProvider.class )
					.withDefault( BeanReference.of( ElasticsearchAwsCredentialsProvider.class,
							ElasticsearchAwsBackendSettings.Defaults.CREDENTIALS_TYPE ) )
					.build();

	private static final OptionalConfigurationProperty<String> LEGACY_ACCESS_KEY =
			ConfigurationProperty.forKey( "aws.signing.access_key" )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<String> LEGACY_SECRET_KEY =
			ConfigurationProperty.forKey( "aws.signing.secret_key" )
					.asString()
					.build();

	static final OptionalConfigurationProperty<ElasticsearchVersion> VERSION =
			ConfigurationProperty.forKey( "version" )
					.as( ElasticsearchVersion.class, ElasticsearchVersion::of )
					.build();

	@Override
	public Optional<ElasticsearchRequestInterceptor> provide(ElasticsearchRequestInterceptorProviderContext context) {
		return configuration( context.beanResolver(), context.configurationPropertySource() )
				.map( config -> new ElasticsearchAwsSigningRequestInterceptor( config.region(), config.service(),
						config.credentials() ) );
	}

	@Override
	public Optional<BeanHolder<ElasticsearchRequestPreparation>> provide(
			ElasticsearchRequestPreparationProviderContext context) {
		return configuration( context.beanResolver(), context.configurationPropertySource() ).map( config -> {
			ElasticsearchRequestPreparation signing = new ElasticsearchAwsRequestSigning(
					config.credentials(), config.region().id(), config.service(), Clock.systemUTC() );
			var holder = BeanHolder.of( signing );
			if ( config.credentials() instanceof AutoCloseable closeable ) {
				return holder.withDependencyAutoClosing( BeanHolder.ofCloseable( closeable ) );
			}
			return holder;
		} );
	}

	private Optional<SigningConfiguration> configuration(BeanResolver beanResolver,
			ConfigurationPropertySource propertySource) {
		if ( !SIGNING_ENABLED.get( propertySource ) ) {
			AwsLog.INSTANCE.signingDisabled();
			return Optional.empty();
		}

		Region region = REGION.getAndMapOrThrow( propertySource, Region::of, AwsLog.INSTANCE::missingPropertyForSigning );
		String service;

		String distributionName = VERSION.get( propertySource )
				.map( ElasticsearchVersion::distribution )
				.map( ElasticsearchDistributionName::externalRepresentation )
				.orElse( "opensearch" );

		if ( "amazon-opensearch-serverless".equals( distributionName ) ) {
			service = "aoss";
		}
		else {
			service = "es";
		}

		AwsCredentialsProvider credentialsProvider = createCredentialsProvider( beanResolver, propertySource );

		AwsLog.INSTANCE.signingEnabled( region, service, credentialsProvider );

		return Optional.of( new SigningConfiguration( region, service, credentialsProvider ) );
	}

	private record SigningConfiguration(Region region, String service, AwsCredentialsProvider credentials) {
	}

	private AwsCredentialsProvider createCredentialsProvider(BeanResolver beanResolver,
			ConfigurationPropertySource propertySource) {
		if ( LEGACY_ACCESS_KEY.get( propertySource ).isPresent()
				|| LEGACY_SECRET_KEY.get( propertySource ).isPresent() ) {
			throw AwsLog.INSTANCE.obsoleteAccessKeyIdOrSecretAccessKeyForSigning(
					LEGACY_ACCESS_KEY.resolveOrRaw( propertySource ),
					LEGACY_SECRET_KEY.resolveOrRaw( propertySource ),
					CREDENTIALS_TYPE.resolveOrRaw( propertySource ),
					ElasticsearchAwsCredentialsTypeNames.STATIC,
					ElasticsearchAwsStaticCredentialsProvider.CREDENTIALS_ACCESS_KEY_ID.resolveOrRaw( propertySource ),
					ElasticsearchAwsStaticCredentialsProvider.CREDENTIALS_SECRET_ACCESS_KEY.resolveOrRaw( propertySource ) );
		}

		try ( BeanHolder<? extends ElasticsearchAwsCredentialsProvider> credentialsProviderHolder =
				CREDENTIALS_TYPE.getAndTransform( propertySource, beanResolver::resolve ) ) {
			return credentialsProviderHolder.get().create( propertySource );
		}
	}

}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.performance.backend.base.profiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.profile.ExternalProfiler;
import org.openjdk.jmh.profile.ProfilerException;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;

public class JfrProfiler implements ExternalProfiler {

	private final Path outputDir;
	private final String jfrOptions;

	private Path dump;

	public JfrProfiler(String initLine) throws ProfilerException {
		Map<String, String> options = new LinkedHashMap<>();
		String[] split = initLine.split( ";" );
		for ( String keyValue : split ) {
			String[] keyValueSplit = keyValue.split( "=" );
			String key = keyValueSplit[0];
			String value = keyValueSplit[1];
			options.put( key, value );
		}

		this.outputDir = Paths.get( Optional.ofNullable( options.remove( "outputDir" ) ).orElse( "." ) );
		this.jfrOptions = Optional.ofNullable( options.remove( "jfrOptions" ) ).orElse( "settings=profile,maxsize=30M" );

		if ( !options.isEmpty() ) {
			throw new ProfilerException( "Unknown options: " + options );
		}
	}

	@Override
	public String getDescription() {
		return "Java Flight Recorder";
	}

	@Override
	public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
		return Collections.emptyList();
	}

	@Override
	public Collection<String> addJVMOptions(BenchmarkParams params) {
		dump = generateDumpPath( params );
		List<String> options = new ArrayList<>();
		options.add( "dumponexit=true" );
		options.add( jfrOptions );
		options.add( "filename=" + dump );
		String optionsString = String.join( ",", options );
		return Arrays.asList(
				"-XX:+FlightRecorder",
				"-XX:StartFlightRecording=" + optionsString
		);
	}

	@Override
	public void beforeTrial(BenchmarkParams benchmarkParams) {
		// Nothing to do
	}

	@Override
	public Collection<? extends Result<?>> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr) {
		//CHECKSTYLE:OFF
		System.out.println( "Java Flight Recording dumped to " + dump );
		//CHECKSTYLE:ON
		return Collections.emptyList();
	}

	@Override
	public boolean allowPrintOut() {
		return true;
	}

	@Override
	public boolean allowPrintErr() {
		return true;
	}

	private Path generateDumpPath(BenchmarkParams params) {
		StringBuilder paramString = new StringBuilder();
		for ( String key : params.getParamsKeys() ) {
			if ( paramString.length() > 0 ) {
				paramString.append( "-" );
			}
			paramString.append( key ).append( "-" ).append( params.getParam( key ) );
		}
		// Use sub-directories to avoid hitting the filename limit
		Path dumpDir = outputDir.resolve( params.getBenchmark() );
		try {
			Files.createDirectories( dumpDir );
		}
		catch (IOException e) {
			throw new IllegalStateException( "Could not create directory " + dumpDir, e );
		}
		return dumpDir.resolve( paramString.toString() + ".jfr" );
	}
}

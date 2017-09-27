/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.scenario;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DurationFormatUtils;

import org.hibernate.search.engine.Version;
import org.hibernate.search.test.performance.task.AbstractTask;
import org.hibernate.search.test.performance.util.CheckerLuceneIndex;
import org.hibernate.search.test.performance.util.TargetDirHelper;
import org.hibernate.search.testsupport.TestConstants;

import static org.apache.commons.lang.StringUtils.leftPad;
import static org.apache.commons.lang.StringUtils.rightPad;
import static org.hibernate.search.test.performance.TestRunnerArquillian.RUNNER_PROPERTIES;
import static org.hibernate.search.test.performance.TestRunnerArquillian.TARGET_DIR_KEY;
import static org.hibernate.search.test.performance.util.Util.runGarbageCollectorAndWait;

/**
 * @author Tomas Hradec
 */
public class TestReporter {

	private TestReporter() {
	}

	public static void printReport(TestContext testCtx, TestScenarioContext warmupCtx, TestScenarioContext measureCtx)
			throws UnsupportedEncodingException, IOException {
		PrintStream outStream = createOutputStream( measureCtx.scenario.getClass().getSimpleName() );
		PrintWriter outWriter = new PrintWriter(
				new BufferedWriter( new OutputStreamWriter( outStream, "UTF-8" ) ), false );

		outWriter.println( "==================================================================" );
		outWriter.println( "HIBERNATE SEARCH PERFORMANCE TEST REPORT" );
		printSummary( testCtx, warmupCtx, measureCtx, outWriter );
		printTaskInfo( measureCtx, outWriter );
		printEnvInfo( testCtx, outWriter );
		outWriter.println( "==================================================================" );
		outWriter.flush();

		CheckerLuceneIndex.printIndexReport( testCtx, outStream );

		Collection<Throwable> warmupFailures = warmupCtx.getFailures();
		Collection<Throwable> measureFailures = measureCtx.getFailures();
		if ( !warmupFailures.isEmpty() || !measureFailures.isEmpty() ) {
			outWriter.println( "===========================================================================" );
			outWriter.println( "EXCEPTIONS" );
			outWriter.println( "" );
			for ( Throwable e : warmupFailures ) {
				e.printStackTrace( outWriter );
				outWriter.println( "---------------------------------------------------------------" );
			}
			for ( Throwable e : measureFailures ) {
				e.printStackTrace( outWriter );
				outWriter.println( "---------------------------------------------------------------" );
			}
		}

		outWriter.close();
		outStream.close();
	}

	private static void printSummary(
			TestContext testCtx, TestScenarioContext warmupCtx, TestScenarioContext measureCtx, PrintWriter out) {
		long freeMemory2 = -1;
		long totalMemory2 = -1;
		if ( testCtx.measureMemory ) {
			runGarbageCollectorAndWait();
			freeMemory2 = Runtime.getRuntime().freeMemory();
			totalMemory2 = Runtime.getRuntime().totalMemory();
		}

		out.println( "" );
		out.println( "SUMMARY" );
		out.println( "    Name   : " + measureCtx.scenario.getClass().getSimpleName() );
		out.println( "    Date   : " + DateFormatUtils.format( new Date(), "yyyy-MM-dd HH:mm" ) );
		out.println( "" );
		out.println( "    Measured time (HH:mm:ss.SSS)" );
		out.println( "        MEASURED TASKS : " + DurationFormatUtils.formatDuration( measureCtx.executionStopWatch.elapsed( TimeUnit.MILLISECONDS ), "HH:mm:ss.SSS" ) );
		out.println( "        init database  : " + DurationFormatUtils.formatDuration( testCtx.initDatabaseStopWatch.elapsed( TimeUnit.MILLISECONDS ), "HH:mm:ss.SSS" ) );
		out.println( "        init index     : " + DurationFormatUtils.formatDuration( testCtx.initIndexStopWatch.elapsed( TimeUnit.MILLISECONDS ), "HH:mm:ss.SSS" ) );
		out.println( "        warmup phase   : " + DurationFormatUtils.formatDuration( warmupCtx.executionStopWatch.elapsed( TimeUnit.MILLISECONDS ), "HH:mm:ss.SSS" ) );

		if ( testCtx.measureMemory ) {
			out.println( "" );
			out.println( "    Memory usage (total-free):" );
			out.println( "        before : " + toMB( measureCtx.initialTotalMemory - measureCtx.initialFreeMemory ) );
			out.println( "        after  : " + toMB( totalMemory2 - freeMemory2 ) );
		}
	}

	private static void printTaskInfo(TestScenarioContext ctx, PrintWriter out) {
		out.println( "" );
		out.println( "TASKS" );
		for ( AbstractTask task : ctx.tasks ) {
			String taskTotalTime = "n/a";
			String taskAverageTime = "n/a";
			if ( ctx.testContext.measureTaskTime ) {
				long taskTotalMilis = TimeUnit.MILLISECONDS.convert( task.getTimerValue(), TimeUnit.NANOSECONDS );
				long taskCount = task.getCounterValue();
				taskTotalTime = DurationFormatUtils.formatDuration( taskTotalMilis, "mm:ss.SSS" );
				if ( taskCount == 0L ) {
					taskAverageTime = "n/a";
				}
				else {
					taskAverageTime = DurationFormatUtils.formatDuration( taskTotalMilis / taskCount, "mm:ss.SSS" );
				}
			}
			out.println( "    "
					+ leftPad( task.getCounterValue() + "x ", 5 )
					+ rightPad( task.getClass().getSimpleName(), 35 )
					+ " | sum " + taskTotalTime
					+ " | avg " + taskAverageTime );
		}
	}

	private static void printEnvInfo(TestContext testCtx, PrintWriter out) {
		out.println( "" );
		out.println( "TEST CONFIGURATION" );
		out.println( "    measure performance : " + TestConstants.arePerformanceTestsEnabled() );
		out.println( "    threads             : " + testCtx.threadCount );
		out.println( "    measured cycles     : " + testCtx.measuredCyclesCount );
		out.println( "    warmup cycles       : " + testCtx.warmupCyclesCount );
		out.println( "    initial book count  : " + testCtx.initialBookCount );
		out.println( "    initial author count: " + testCtx.initialAuthorCount );
		out.println( "    verbose             : " + testCtx.verbose );
		out.println( "    measure memory      : " + testCtx.measureMemory );
		out.println( "    measure task time   : " + testCtx.measureTaskTime );
		out.println( "    check query results : " + testCtx.assertQueryResults );
		out.println( "    check index state   : " + testCtx.checkIndexState );

		out.println( "" );
		out.println( "HIBERNATE SEARCH PROPERTIES" );
		Map<String, Object> properties = testCtx.sessionFactory.getProperties();
		for ( Entry<String, Object> e : properties.entrySet() ) {
			if ( e.getKey().toString().startsWith( "hibernate.search" ) ) {
				out.println( "    " + e.getKey() + " = " + e.getValue() );
			}
		}

		out.println( "" );
		out.println( "VERSIONS" );
		out.println( "    org.hibernate.search : " + Version.getVersionString() );
		out.println( "    org.hibernate        : " + org.hibernate.Version.getVersionString() );
		out.println( "" );
	}

	private static String toMB(long bytes) {
		long megabytes = bytes / 1000 / 1000;
		return megabytes + "MB";
	}

	private static PrintStream createOutputStream(String testScenarioName) {
		try {
			Path targetDir = getTargetDir();
			String reportFileName = "report-" + testScenarioName + "-" + DateFormatUtils.format( new Date(), "yyyy-MM-dd-HH'h'mm'm'" ) + ".txt";
			Path reportFile = targetDir.resolve( reportFileName );

			final OutputStream std = System.out;
			final OutputStream file = new PrintStream( new FileOutputStream( reportFile.toFile() ), true, "UTF-8" );
			final OutputStream stream = new OutputStream() {

				@Override
				public void write(int b) throws IOException {
					std.write( b );
					file.write( b );
				}

				@Override
				public void flush() throws IOException {
					std.flush();
					file.flush();
				}

				@Override
				public void close() throws IOException {
					file.close();
				}
			};
			return new PrintStream( stream, false, "UTF-8" );
		}
		catch (FileNotFoundException | UnsupportedEncodingException e) {
			throw new RuntimeException( e );
		}
	}

	private static Path getTargetDir() {
		InputStream runnerPropertiesStream = TestReporter.class.getResourceAsStream( "/" + RUNNER_PROPERTIES );
		if ( runnerPropertiesStream != null ) {
			Properties runnerProperties = new Properties();
			try {
				runnerProperties.load( runnerPropertiesStream );
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
			return Paths.get( runnerProperties.getProperty( TARGET_DIR_KEY ) );
		}
		else {
			return TargetDirHelper.getTargetDir();
		}
	}

}

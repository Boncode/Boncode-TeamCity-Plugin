package nl.ijsberg;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildRunnerContext;
import nl.ijsberg.analysis.server.buildserver.AnalysisPropertiesLoader;
import nl.ijsberg.analysis.server.buildserver.BuildServerToMonitorLink;
import org.ijsberg.iglu.configuration.ConfigurationException;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.logging.Logger;
import org.ijsberg.iglu.util.properties.PropertiesSupport;
import org.ijsberg.iglu.util.reflection.ReflectionSupport;
import org.jetbrains.annotations.NotNull;
import java.util.Properties;

import java.util.Map;

/**
 */
public class BoncodeTeamCityBuildProcess implements BuildProcess {

	private BuildRunnerContext context;

	private String monitorUploadDirectory;
    private String monitorDownloadDirectory;

    private String analysisProperties;
	private String sourceRoot;
	private String checkoutDir;
	private AnalysisPropertiesLoader analysisPropertiesLoader;
	private String alternativePropertiesInSourceRoot;

	private Logger logger;
	private BuildServerToMonitorLink buildServerToMonitorLink;


	public BoncodeTeamCityBuildProcess(BuildRunnerContext context, Logger logger) {
		this.context = context;
		this.logger = logger;
		Map<String, String> configParameters = context.getConfigParameters();
		Map<String, String> buildParameters = context.getConfigParameters();
		logger.log(new LogEntry("config parameters: " + configParameters));
		logger.log(new LogEntry("build parameters: " + buildParameters));
		logger.log(new LogEntry("Checkout dir: " + context.getBuild().getCheckoutDirectory()));

		checkoutDir = context.getBuild().getCheckoutDirectory().getAbsolutePath();
		sourceRoot = checkoutDir + "/" + buildParameters.get(BuildServerToMonitorLink.SOURCE_ROOT);
		monitorUploadDirectory = buildParameters.get(BuildServerToMonitorLink.MONITOR_UPLOAD_DIRECTORY);
        monitorDownloadDirectory = buildParameters.get(BuildServerToMonitorLink.MONITOR_DOWNLOAD_DIRECTORY);

		String analysisPropertiesLoaderClassName = buildParameters.get(BuildServerToMonitorLink.ALTERNATIVE_PROPERTIES_LOADER);
		if(analysisPropertiesLoaderClassName != null) {
			try {
				analysisPropertiesLoader = (AnalysisPropertiesLoader)ReflectionSupport.instantiateClass(analysisPropertiesLoaderClassName);
			} catch (InstantiationException e) {
				throw new ConfigurationException("cannot instantiate class found in property " + BuildServerToMonitorLink.ALTERNATIVE_PROPERTIES_LOADER, e);
			}
			alternativePropertiesInSourceRoot = buildParameters.get(BuildServerToMonitorLink.ALTERNATIVE_PROPERTIES_IN_SOURCE_ROOT);
			BuildServerToMonitorLink.checkFile(
					BuildServerToMonitorLink.ALTERNATIVE_PROPERTIES_IN_SOURCE_ROOT,
					alternativePropertiesInSourceRoot,
					false);
		} else {
			logger.log(new LogEntry("no value found for property " + BuildServerToMonitorLink.ALTERNATIVE_PROPERTIES_LOADER + " using standard properties loader"));
		}
        analysisProperties = buildParameters.get(BuildServerToMonitorLink.ANALYSIS_PROPERTIES_FILENAME);
		BuildServerToMonitorLink.throwIfPropertiesNotOk(
				analysisProperties,
				monitorUploadDirectory,
				sourceRoot);
		Properties properties = loadProperties();
		buildServerToMonitorLink = new BuildServerToMonitorLink(properties, monitorUploadDirectory, monitorDownloadDirectory, logger);
	}

	private Properties loadProperties() {
		if(analysisPropertiesLoader != null) {
			return analysisPropertiesLoader.load(analysisProperties, alternativePropertiesInSourceRoot);
		}
		return PropertiesSupport.loadProperties(analysisProperties);
	}

	public void start() throws RunBuildException {
		logger.log(new LogEntry("Starting Boncode analysis"));
        boolean result = buildServerToMonitorLink.perform(sourceRoot);
        if(result){
            this.isFinished();
        } else {
            this.isInterrupted();
        }
	}

	public boolean isInterrupted() {
		logger.log(new LogEntry("isInterrupted invoked"));
		return false;
	}

	public boolean isFinished() {
		logger.log(new LogEntry("isFinished invoked"));
		return false;
	}

	public void interrupt() {
		logger.log(new LogEntry("interrupt invoked"));
	}

	@NotNull
	public BuildFinishedStatus waitFor() throws RunBuildException {
		logger.log(new LogEntry("waitFor invoked"));
		return BuildFinishedStatus.FINISHED_SUCCESS;
	}

}

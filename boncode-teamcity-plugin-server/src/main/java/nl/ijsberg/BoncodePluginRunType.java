package nl.ijsberg;

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 */
public class BoncodePluginRunType extends RunType implements PropertiesProcessor {

	public BoncodePluginRunType(@NotNull final RunTypeRegistry registry,
								@NotNull final PluginDescriptor descriptor) {
		registry.registerRunType(this);
	}

	@NotNull
	@Override
	public String getType() {
		return "Boncode";
	}

	@NotNull
	@Override
	public String getDisplayName() {
		return "Boncode";  
	}

	@NotNull
	@Override
	public String getDescription() {
		return "Boncode"; 
	}

	@Nullable
	@Override
	public PropertiesProcessor getRunnerPropertiesProcessor() {
		return null;  
	}

	@Nullable
	@Override
	public String getEditRunnerParamsJspFilePath() {
		return null;  
	}

	@Nullable
	@Override
	public String getViewRunnerParamsJspFilePath() {
		return null;
	}

	@Nullable
	@Override
	public Map<String, String> getDefaultRunnerProperties() {
		return null;
	}



	public Collection<InvalidProperty> process(Map<String, String> properties) {
		Collection<InvalidProperty> result = new HashSet<InvalidProperty>();
		if (StringUtil.isEmptyOrSpaces(properties.get("analysis_properties_location"))) {
			result.add(new InvalidProperty("analysis_properties_location", "Please specify the location of the analysis properties file"));
		}
		return result;
	}
}

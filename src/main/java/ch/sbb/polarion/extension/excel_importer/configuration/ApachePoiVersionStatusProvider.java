package ch.sbb.polarion.extension.excel_importer.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import org.apache.poi.Version;
import org.jetbrains.annotations.NotNull;

@Discoverable
@SuppressWarnings("unused")
public class ApachePoiVersionStatusProvider extends ConfigurationStatusProvider {

    private static final String APACHE_POI_NAME = "Apache POI";
    private static final String VERSION_LABEL_TEMPLATE = "Version %s";

    @Override
    public @NotNull ConfigurationStatus getStatus(@NotNull Context context) {
        return new ConfigurationStatus(APACHE_POI_NAME, Status.OK, VERSION_LABEL_TEMPLATE.formatted(Version.getVersion()));
    }
}

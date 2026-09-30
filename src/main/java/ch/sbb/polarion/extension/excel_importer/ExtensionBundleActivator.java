package ch.sbb.polarion.extension.excel_importer;

import ch.sbb.polarion.extension.excel_importer.service.ImportJobsService;
import ch.sbb.polarion.extension.generic.GenericBundleActivator;
import com.polarion.alm.ui.server.forms.extensions.IFormExtension;
import org.osgi.framework.BundleContext;

import java.util.Map;

/**
 * Stops the import jobs with the bundle: a running import is asked to stop at its next row, and one which waits for a
 * thread is cancelled.
 */
public class ExtensionBundleActivator extends GenericBundleActivator {

    @Override
    protected Map<String, IFormExtension> getExtensions() {
        return Map.of();
    }

    @Override
    public void stop(BundleContext context) {
        ImportJobsService.shutdown();
        super.stop(context);
    }

}

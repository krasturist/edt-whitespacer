package ru.nechaev.edt.whitespacetrimmer;

import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "ru.nechaev.edt.whitespacetrimmer";

    private static Activator plugin;

    private ICommandService commandService;
    private IExecutionListener saveListener;

    public static Activator getDefault()
    {
        return plugin;
    }

    @Override
    public void start(BundleContext context) throws Exception
    {
        super.start(context);
        plugin = this;
    }

    public void registerSaveListener(ICommandService service, IExecutionListener listener)
    {

        commandService = service;
        saveListener = listener;
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        try
        {
            if (commandService != null && saveListener != null)
            {
                commandService.removeExecutionListener(saveListener);
            }

            commandService = null;
            saveListener = null;
            plugin = null;
        }
        finally
        {
            super.stop(context);
        }
    }
}
package ru.nechaev.edt.whitespacetrimmer;

import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

public final class Startup
    implements IStartup
{

    @Override
    public void earlyStartup()
    {
        PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
            if (PlatformUI.getWorkbench().isClosing())
            {
                return;
            }

            ICommandService service = PlatformUI.getWorkbench().getService(ICommandService.class);

            if (service != null)
            {
                SaveListener listener = new SaveListener();
                service.addExecutionListener(listener);
                Activator.getDefault().registerSaveListener(service, listener);
            }
        });
    }
}
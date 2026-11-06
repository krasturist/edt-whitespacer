package ru.nechaev.edt.whitespacetrimmer;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;

public final class PreferenceInitializer
    extends AbstractPreferenceInitializer
{

    @Override
    public void initializeDefaultPreferences()
    {
        DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID)
            .put(PreferenceConstants.PATH_MARKER, PreferenceConstants.DEFAULT_PATH_MARKER);
    }
}
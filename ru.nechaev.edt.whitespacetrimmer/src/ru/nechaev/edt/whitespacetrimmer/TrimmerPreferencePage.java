package ru.nechaev.edt.whitespacetrimmer;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public final class TrimmerPreferencePage
    extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage
{

    public TrimmerPreferencePage()
    {
        super(GRID);

        setPreferenceStore(Activator.getDefault().getPreferenceStore());

        setDescription("Удаление пробелов и табуляций в конце строк " + "при сохранении.\n\n"
            + "Обрабатываются только файлы, полный путь которых " + "содержит указанную подстроку.\n"
            + "Регистр учитывается. " + "Пустое поле отключает очистку.");
    }

    @Override
    protected void createFieldEditors()
    {
        StringFieldEditor markerEditor =
            new StringFieldEditor(PreferenceConstants.PATH_MARKER, "Подстрока в пути файла:", getFieldEditorParent());

        markerEditor.setEmptyStringAllowed(true);

        addField(markerEditor);
    }

    @Override
    public void init(IWorkbench workbench)
    {
    }
}
package ru.nechaev.edt.whitespacetrimmer;

import java.net.URI;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public final class SaveListener
    implements IExecutionListener
{

    private static final String SAVE = "org.eclipse.ui.file.save";
    private static final String SAVE_ALL = "org.eclipse.ui.file.saveAll";

    @Override
    public void preExecute(String commandId, ExecutionEvent event)
    {
        if (!SAVE.equals(commandId) && !SAVE_ALL.equals(commandId))
        {
            return;
        }

        Set<IDocument> processed = Collections.newSetFromMap(new IdentityHashMap<>());

        log("Command: " + commandId);

        if (SAVE.equals(commandId))
        {
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

            if (window != null && window.getActivePage() != null)
            {
                cleanEditor(window.getActivePage().getActiveEditor(), processed);
            }

            return;
        }

        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows())
        {

            for (IWorkbenchPage page : window.getPages())
            {
                for (IEditorReference reference : page.getEditorReferences())
                {

                    IEditorPart editor = reference.getEditor(false);

                    if (editor != null && editor.isDirty())
                    {
                        cleanEditor(editor, processed);
                    }
                }
            }
        }
    }

    private void cleanEditor(IEditorPart editor, Set<IDocument> processed)
    {

        if (editor == null)
        {
            return;
        }

        try
        {
            cleanEditorInternal(editor, processed);
        }
        catch (BadLocationException | RuntimeException exception)
        {
            log("ERROR | editor=" + editor.getClass().getName());
            exception.printStackTrace(System.out);
        }
    }

    private void cleanEditorInternal(IEditorPart editor, Set<IDocument> processed) throws BadLocationException
    {

        ITextEditor textEditor =
            editor instanceof ITextEditor ? (ITextEditor)editor : editor.getAdapter(ITextEditor.class);

        if (textEditor == null)
        {
            log("SKIP | no ITextEditor | " + editor.getClass().getName());
            return;
        }

        IEditorInput input = textEditor.getEditorInput();
        URI uri = resolveUri(input);

        if (uri == null)
        {
            log("SKIP | no URI | input=" + (input == null ? "null" : input.getClass().getName()));
            return;
        }

        if (!"file".equalsIgnoreCase(uri.getScheme()))
        {
            log("SKIP | unsupported URI scheme | " + uri);
            return;
        }

        // getPath() возвращает декодированный путь:
        // кириллица проверяется как текст, а не как %D1%...
        String path = uri.normalize().getPath();

        String marker = Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.PATH_MARKER);

        if (marker == null || marker.isBlank())
        {
            log("SKIP | cleanup disabled: empty path marker");
            return;
        }

        if (path == null || !path.contains(marker))
        {
            log("SKIP | marker not found: " + marker + " | " + path);
            return;
        }

        log("MATCH | " + path);

        if (!textEditor.isEditable())
        {
            log("SKIP | editor is not editable | " + path);
            return;
        }

        IDocumentProvider provider = textEditor.getDocumentProvider();

        if (provider == null)
        {
            log("SKIP | no document provider | " + path);
            return;
        }

        IDocument document = provider.getDocument(input);

        if (document == null)
        {
            log("SKIP | no document | " + path);
            return;
        }

        if (!processed.add(document))
        {
            log("SKIP | document already processed | " + path);
            return;
        }

        IRewriteTarget rewriteTarget = textEditor.getAdapter(IRewriteTarget.class);

        int removed;

        if (rewriteTarget != null)
        {
            rewriteTarget.beginCompoundChange();
        }

        try
        {
            removed = removeTrailingWhitespace(document);
        }
        finally
        {
            if (rewriteTarget != null)
            {
                rewriteTarget.endCompoundChange();
            }
        }

        log("CLEAN | removed=" + removed + " | dirtyBeforeSave=" + textEditor.isDirty() + " | " + path);

    }

    private URI resolveUri(IEditorInput input)
    {
        if (input == null)
        {
            return null;
        }

        if (input instanceof IURIEditorInput uriInput)
        {
            URI uri = uriInput.getURI();

            if (uri != null)
            {
                return uri;
            }
        }

        if (input instanceof IFileEditorInput fileInput)
        {
            IFile file = fileInput.getFile();

            if (file != null)
            {
                return file.getLocationURI();
            }
        }

        IFile file = input.getAdapter(IFile.class);

        if (file != null)
        {
            return file.getLocationURI();
        }

        IURIEditorInput uriInput = input.getAdapter(IURIEditorInput.class);

        return uriInput == null ? null : uriInput.getURI();
    }

    private int removeTrailingWhitespace(IDocument document) throws BadLocationException
    {

        int removed = 0;

        for (int line = document.getNumberOfLines() - 1; line >= 0; line--)
        {

            IRegion region = document.getLineInformation(line);

            int start = region.getOffset();
            int end = start + region.getLength();
            int trimFrom = end;

            while (trimFrom > start)
            {
                char character = document.getChar(trimFrom - 1);

                if (character != ' ' && character != '\t')
                {
                    break;
                }

                trimFrom--;
            }

            if (trimFrom < end)
            {
                int length = end - trimFrom;

                document.replace(trimFrom, length, "");
                removed += length;
            }
        }

        return removed;
    }

    private static void log(String message)
    {
        System.out.println("[WhitespaceTrimmer] " + message);
    }

    @Override
    public void postExecuteSuccess(String commandId, Object returnValue)
    {
    }

    @Override
    public void postExecuteFailure(String commandId, ExecutionException exception)
    {

        if (SAVE.equals(commandId) || SAVE_ALL.equals(commandId))
        {
            log("Save command failed: " + commandId);
            exception.printStackTrace(System.out);
        }
    }

    @Override
    public void notHandled(String commandId, NotHandledException exception)
    {
    }
}
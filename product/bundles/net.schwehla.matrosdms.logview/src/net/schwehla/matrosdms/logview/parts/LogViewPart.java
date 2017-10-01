package net.schwehla.matrosdms.logview.parts;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.nls.Translation;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import net.schwehla.matrosdms.i18n.MatrosMessage;
import net.schwehla.matrosdms.logview.dialogs.DisplayTextDialog;
import net.schwehla.matrosdms.logview.util.LogEntry;
import net.schwehla.matrosdms.logview.util.LogEntryCache;
import net.schwehla.matrosdms.logview.util.UIConstants;




public class LogViewPart {

	
	@Inject
	@Translation
	MatrosMessage messages;

	

	    private List<LogEntry> entries = new ArrayList<LogEntry>();

	    private TreeViewer logViewer;


	    @PostConstruct
	    public void createComposite(Composite parent, LogEntryCache cache)
	    {
	        this.entries = cache.getEntries();

	        Composite container = new Composite(parent, SWT.NONE);
	        TreeColumnLayout layout = new TreeColumnLayout();
	        container.setLayout(layout);

	        logViewer = new TreeViewer(container, SWT.FULL_SELECTION);

	        TreeViewerColumn column = new TreeViewerColumn(logViewer, SWT.NONE);
	        column.getColumn().setText(messages.logviewpart_column_date);
	        layout.setColumnData(column.getColumn(), new ColumnPixelData(140));
	        column.setLabelProvider(new ColumnLabelProvider()
	        {
	            private DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

	            @Override
	            public String getText(Object element)
	            {
	                return dateFormat.format(((LogEntry) element).getDate());
	            }
	        });

	        TreeViewerColumn columnSeverity = new TreeViewerColumn(logViewer, SWT.NONE);
	        columnSeverity.getColumn().setText(messages.logviewpart_column_severity);
	        layout.setColumnData(columnSeverity.getColumn(), new ColumnPixelData(140));
	        columnSeverity.setLabelProvider(new ColumnLabelProvider()
	        {

	            @Override
	            public String getText(Object element)
	            {
	                return "" + ((LogEntry) element).getSeverity();
	            }
	        });
	        
	        
	        column = new TreeViewerColumn(logViewer, SWT.NONE);
	        column.getColumn().setText(messages.logviewpart_column_message);
	        layout.setColumnData(column.getColumn(), new ColumnPixelData(500));
	        column.setLabelProvider(new ColumnLabelProvider()
	        {
	            @Override
	            public String getText(Object element)
	            {
	                return ((LogEntry) element).getMessage();
	            }

	            @Override
	            public Image getImage(Object element)
	            {
	                LogEntry entry = (LogEntry) element;

	                switch (entry.getSeverity())
	                {
//	                    case IStatus.ERROR:
//	                        return Images.ERROR.image();
//	                    case IStatus.WARNING:
//	                        return Images.WARNING.image();
//	                    default:
//	                        return Images.INFO.image();
	                }
	                // XXX
	                return null;
	            }
	        });

	        logViewer.getTree().setHeaderVisible(true);
	        logViewer.getTree().setLinesVisible(true);

	        logViewer.setContentProvider(new LogEntryContentProvider());

	        logViewer.setInput(entries);

	        logViewer.addDoubleClickListener(new IDoubleClickListener()
	        {
	            @Override
	            public void doubleClick(DoubleClickEvent event)
	            {
	                LogEntry entry = (LogEntry) ((IStructuredSelection) event.getSelection()).getFirstElement();
	                DisplayTextDialog dialog = new DisplayTextDialog(Display.getCurrent().getActiveShell(), entry.getText());
	                dialog.open();
	            }
	        });
	    }

	    @Focus
	    public void setFocus()
	    {
	        logViewer.getTree().setFocus();
	    }

	    @Inject
	    @Optional
	    public void onLogEntryCreated(@UIEventTopic(UIConstants.Event.Log.CREATED) LogEntry entry)
	    {

	        // add in front
	    	entries.add(0, entry);
	    	//entries.add(entry);
	        logViewer.refresh();
	    }

	    @Inject
	    @Optional
	    public void onLogEntriesDeleted(@UIEventTopic(UIConstants.Event.Log.CLEARED) LogEntry entry)
	    {
	        entries.clear();
	        logViewer.refresh();
	    }

	    public static class LogEntryContentProvider implements ITreeContentProvider
	    {
	        private List<?> entries;

	        @Override
	        public void dispose()
	        {}

	        @Override
	        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	        {
	            this.entries = (List<?>) newInput;
	        }

	        @Override
	        public Object[] getElements(Object inputElement)
	        {
	            return entries.toArray();
	        }

	        @Override
	        public boolean hasChildren(Object element)
	        {
	            return ((LogEntry) element).getChildren() != null;
	        }

	        @Override
	        public Object[] getChildren(Object parentElement)
	        {
	            return ((LogEntry) parentElement).getChildren().toArray();
	        }

	        @Override
	        public Object getParent(Object element)
	        {
	            return null;
	        }
	    }
	    
	    
}

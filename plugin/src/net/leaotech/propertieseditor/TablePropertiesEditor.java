package net.leaotech.propertieseditor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map.Entry;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

public class TablePropertiesEditor extends EditorPart {
	public static final String ID = TablePropertiesEditor.class.getName();
	private static final String KEY_PROPERTY = "Key";
	private static final String VALUE_PROPERTY = "Value";
	private final Properties properties = new Properties();
	private transient boolean isDirty;
	private TableViewer viewer;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());
		try {
			IFile file = ((IFileEditorInput) input).getFile();
			properties.load(file.getContents());
			isDirty = false;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		viewer.setContentProvider(ArrayContentProvider.getInstance());

		Table table = viewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		TableViewerColumn keyColumn = new TableViewerColumn(viewer, SWT.LEFT);
		keyColumn.getColumn().setText(KEY_PROPERTY);
		keyColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return String.valueOf(((Entry<?, ?>) element).getKey());
			}
		});
		TableViewerColumn valueColumn = new TableViewerColumn(viewer, SWT.LEFT);
		valueColumn.getColumn().setText(VALUE_PROPERTY);
		valueColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return String.valueOf(((Entry<?, ?>) element).getValue());
			}
		});

		TableColumnLayout columnLayout = new TableColumnLayout();
		columnLayout.setColumnData(keyColumn.getColumn(), new ColumnPixelData(200));
		columnLayout.setColumnData(valueColumn.getColumn(), new ColumnWeightData(200, 100));
		parent.setLayout(columnLayout);

		viewer.setCellEditors(
				new CellEditor[] { new TextCellEditor(table, SWT.NONE), new TextCellEditor(table, SWT.NONE) });
		viewer.setColumnProperties(new String[] { KEY_PROPERTY, VALUE_PROPERTY });
		viewer.setCellModifier(new PropertiesCellModifier());
		viewer.setInput(properties.entrySet());

		MenuManager popupMenu = new MenuManager();
		IAction newEntryAction = new Action() {
			public void run() {
				properties.put("", "");
				viewer.refresh();
			}
		};
		newEntryAction.setText("Insert new Entry");
		popupMenu.add(newEntryAction);
		Menu menu = popupMenu.createContextMenu(table);
		table.setMenu(menu);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			properties.store(stream, null);
			IFile file = ((IFileEditorInput) this.getEditorInput()).getFile();
			file.setContents(new ByteArrayInputStream(stream.toByteArray()), true, true, null);
			setDirty(false);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void doSaveAs() {
		doSave(null);
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}

	public Properties getProperties() {
		return properties;
	}

	public TableViewer getViewer() {
		return viewer;
	}

	private void setDirty(boolean value) {
		isDirty = value;
		firePropertyChange(PROP_DIRTY);
	}

	private class PropertiesCellModifier implements ICellModifier {
		@Override
		public boolean canModify(Object element, String property) {
			return true;
		}

		@Override
		public Object getValue(Object element, String property) {
			if (KEY_PROPERTY.equals(property))
				return ((Entry<?, ?>) element).getKey();
			else
				return ((Entry<?, ?>) element).getValue();
		}

		@Override
		public void modify(Object element, String property, Object value) {
			if (element == null)
				return;
			TableItem tableItem = (TableItem) element;
			Entry<?, ?> data = (Entry<?, ?>) tableItem.getData();
			String svalue = value.toString();
			boolean propChanged = false;
			if (data == null)
				return;
			if (KEY_PROPERTY.equals(property)) {
				if (!properties.containsKey(svalue)) {
					properties.put(svalue, properties.remove(data.getKey()));
					propChanged = true;
				}
			} else {
				if (!properties.get(data.getKey()).toString().equals(svalue)) {
					properties.setProperty((String) data.getKey(), svalue);
					propChanged = true;
				}
			}
			if (propChanged) {
				viewer.refresh();
				setDirty(true);
			}
		}
	}
}

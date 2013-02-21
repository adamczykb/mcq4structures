package pl.poznan.put.cs.bioserver.gui.windows;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import pl.poznan.put.cs.bioserver.helper.StructureManager;

class DialogStructures extends JDialog {
    public static final int CANCEL = 0;
    public static final int OK = 1;
    private static final long serialVersionUID = 1L;

    private static int chosenOption;
    private static DialogStructures instance;
    private static DefaultListModel<File> modelAll;
    private static DefaultListModel<File> modelSelected;
    private static File[] selectedStructures;

    public static File[] getFiles() {
        return DialogStructures.selectedStructures;
    }

    public static DialogStructures getInstance(Frame owner) {
        if (DialogStructures.instance == null) {
            DialogStructures.instance = new DialogStructures(owner);
        }
        return DialogStructures.instance;
    }

    public static String getSelectionDescription() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < DialogStructures.selectedStructures.length; i++) {
            builder.append(StructureManager
                    .getName(DialogStructures.selectedStructures[i]));
            if (i != DialogStructures.selectedStructures.length - 1) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    public static int showDialog() {
        Set<File> setManager = StructureManager.getAllStructures();
        Set<File> setLeft = new HashSet<>();
        Set<File> setRight = new HashSet<>();

        DialogStructures.fillCollection(DialogStructures.modelAll.elements(),
                setLeft);
        DialogStructures.fillCollection(
                DialogStructures.modelSelected.elements(), setRight);

        Set<File> set = new HashSet<>(setLeft);
        set.removeAll(setManager);
        for (File file : set) {
            DialogStructures.modelAll.removeElement(file);
        }

        set = new HashSet<>(setRight);
        set.removeAll(setManager);
        for (File file : set) {
            DialogStructures.modelSelected.removeElement(file);
        }

        set = new HashSet<>(setManager);
        set.removeAll(setLeft);
        set.removeAll(setRight);
        for (File file : set) {
            DialogStructures.modelAll.addElement(file);
        }

        DialogStructures.chosenOption = DialogStructures.CANCEL;
        DialogStructures.instance.setVisible(true);
        return DialogStructures.chosenOption;
    }

    private static void fillCollection(Enumeration<File> enumeration,
            Collection<File> collection) {
        while (enumeration.hasMoreElements()) {
            collection.add(enumeration.nextElement());
        }
    }

    private DialogStructures(Frame owner) {
        super(owner, true);

        DialogStructures.modelAll = new DefaultListModel<>();
        final JList<File> listAll = new JList<>(DialogStructures.modelAll);
        listAll.setBorder(BorderFactory
                .createTitledBorder("Available structures"));
        final ListCellRenderer<? super File> renderer = listAll
                .getCellRenderer();
        DialogStructures.modelSelected = new DefaultListModel<>();
        final JList<File> listSelected = new JList<>(
                DialogStructures.modelSelected);
        listSelected.setBorder(BorderFactory
                .createTitledBorder("Selected structures"));

        ListCellRenderer<File> pdbCellRenderer = new ListCellRenderer<File>() {
            @Override
            public Component getListCellRendererComponent(
                    JList<? extends File> list, File value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) renderer.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                label.setText(StructureManager.getName(value));
                return label;
            }
        };
        listAll.setCellRenderer(pdbCellRenderer);
        listSelected.setCellRenderer(pdbCellRenderer);

        final JButton buttonSelect = new JButton("Select ->");
        buttonSelect.setEnabled(false);
        final JButton buttonSelectAll = new JButton("Select all ->");
        final JButton buttonDeselect = new JButton("<- Deselect");
        buttonDeselect.setEnabled(false);
        JButton buttonDeselectAll = new JButton("<- Deselect all");

        JPanel panelButtons = new JPanel();
        panelButtons.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panelButtons.add(buttonSelect, constraints);
        constraints.gridy++;
        panelButtons.add(buttonSelectAll, constraints);
        constraints.gridy++;
        panelButtons.add(buttonDeselect, constraints);
        constraints.gridy++;
        panelButtons.add(buttonDeselectAll, constraints);

        JPanel panelMain = new JPanel();
        panelMain.setLayout(new GridBagLayout());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.5;
        constraints.weighty = 0.5;
        constraints.fill = GridBagConstraints.BOTH;
        panelMain.add(new JScrollPane(listAll), constraints);
        constraints.gridx++;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.VERTICAL;
        panelMain.add(panelButtons, constraints);
        constraints.gridx++;
        constraints.weightx = 0.5;
        constraints.fill = GridBagConstraints.BOTH;
        panelMain.add(new JScrollPane(listSelected), constraints);

        JButton buttonOk = new JButton("OK");
        JButton buttonCancel = new JButton("Cancel");
        JPanel panelOkCancel = new JPanel();
        panelOkCancel.add(buttonOk);
        panelOkCancel.add(buttonCancel);

        setLayout(new BorderLayout());
        add(panelMain, BorderLayout.CENTER);
        add(panelOkCancel, BorderLayout.SOUTH);

        int width = 640;
        int height = 480;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width - width;
        int y = screenSize.height - height;
        setSize(width, height);
        setLocation(x / 2, y / 2);

        setTitle("MCQ4Structures: structure selection");

        ListSelectionListener listSelectionListener = new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent arg0) {
                ListSelectionModel source = (ListSelectionModel) arg0
                        .getSource();
                if (source.equals(listAll.getSelectionModel())) {
                    buttonSelect.setEnabled(!listAll.isSelectionEmpty());
                } else { // source.equals(listSelected)
                    buttonDeselect.setEnabled(!listSelected.isSelectionEmpty());
                }
            }
        };
        listAll.getSelectionModel().addListSelectionListener(
                listSelectionListener);
        listSelected.getSelectionModel().addListSelectionListener(
                listSelectionListener);

        ActionListener actionListenerSelectDeselect = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                List<File> values;
                boolean isSelect;

                Object source = arg0.getSource();
                if (source.equals(buttonSelect)) {
                    values = listAll.getSelectedValuesList();
                    isSelect = true;
                } else if (source.equals(buttonSelectAll)) {
                    values = new ArrayList<>();
                    Enumeration<File> elements = DialogStructures.modelAll
                            .elements();
                    while (elements.hasMoreElements()) {
                        values.add(elements.nextElement());
                    }
                    isSelect = true;
                } else if (source.equals(buttonDeselect)) {
                    values = listSelected.getSelectedValuesList();
                    isSelect = false;
                } else { // source.equals(buttonSelectAll)
                    values = new ArrayList<>();
                    Enumeration<File> elements = DialogStructures.modelSelected
                            .elements();
                    while (elements.hasMoreElements()) {
                        values.add(elements.nextElement());
                    }
                    isSelect = false;
                }

                for (File f : values) {
                    if (isSelect) {
                        DialogStructures.modelAll.removeElement(f);
                        DialogStructures.modelSelected.addElement(f);
                    } else {
                        DialogStructures.modelAll.addElement(f);
                        DialogStructures.modelSelected.removeElement(f);
                    }
                }
            }
        };
        buttonSelect.addActionListener(actionListenerSelectDeselect);
        buttonSelectAll.addActionListener(actionListenerSelectDeselect);
        buttonDeselect.addActionListener(actionListenerSelectDeselect);
        buttonDeselectAll.addActionListener(actionListenerSelectDeselect);

        buttonOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<File> list = new ArrayList<>();
                DialogStructures.fillCollection(
                        DialogStructures.modelSelected.elements(), list);
                DialogStructures.selectedStructures = list
                        .toArray(new File[list.size()]);

                DialogStructures.chosenOption = DialogStructures.OK;
                dispose();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                DialogStructures.chosenOption = DialogStructures.CANCEL;
                dispose();
            }
        });
    }
}

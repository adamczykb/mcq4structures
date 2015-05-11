package pl.poznan.put.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;

import pl.poznan.put.pdb.analysis.MoleculeType;
import pl.poznan.put.pdb.analysis.PdbCompactFragment;

public class FilteredListModel extends AbstractListModel<PdbCompactFragment> {
    private static final long serialVersionUID = 1L;

    boolean isProtein = true;
    boolean isRNA = true;
    List<PdbCompactFragment> listProteins = new ArrayList<>();
    List<PdbCompactFragment> listRNAs = new ArrayList<>();

    public FilteredListModel() {
    }

    @Override
    public PdbCompactFragment getElementAt(int index) {
        if (isRNA) {
            if (index < listRNAs.size()) {
                return listRNAs.get(index);
            }
            return listProteins.get(index - listRNAs.size());
        }
        return listProteins.get(index);
    }

    @Override
    public int getSize() {
        return (isRNA ? listRNAs.size() : 0) + (isProtein ? listProteins.size() : 0);
    }

    public List<PdbCompactFragment> getElements() {
        ArrayList<PdbCompactFragment> list = new ArrayList<>();
        list.addAll(listRNAs);
        list.addAll(listProteins);
        return list;
    }

    public List<PdbCompactFragment> getSelectedElements() {
        List<PdbCompactFragment> list = new ArrayList<>();
        if (isRNA) {
            list.addAll(listRNAs);
        }
        if (isProtein) {
            list.addAll(listProteins);
        }
        return list;
    }

    public void addElement(PdbCompactFragment element) {
        MoleculeType moleculeType = element.getMoleculeType();

        if (moleculeType == MoleculeType.RNA) {
            listRNAs.add(element);
        } else if (moleculeType == MoleculeType.PROTEIN) {
            listProteins.add(element);
        }
    }

    public void addElements(List<PdbCompactFragment> list) {
        for (PdbCompactFragment element : list) {
            addElement(element);
        }
    }

    public void removeElement(PdbCompactFragment element) {
        if (listRNAs.contains(element)) {
            listRNAs.remove(element);
        } else {
            listProteins.remove(element);
        }
    }

    public void removeElements(List<PdbCompactFragment> list) {
        for (PdbCompactFragment element : list) {
            removeElement(element);
        }
    }

    public boolean canAddElement(PdbCompactFragment element) {
        MoleculeType moleculeType = element.getMoleculeType();
        if (getSize() > 0 && getElementAt(0).getMoleculeType() != moleculeType) {
            return false;
        }

        List<PdbCompactFragment> list = moleculeType == MoleculeType.RNA ? listRNAs : listProteins;
        if (list.size() > 0 && list.get(0).size() != element.size()) {
            return false;
        }

        return true;
    }
}

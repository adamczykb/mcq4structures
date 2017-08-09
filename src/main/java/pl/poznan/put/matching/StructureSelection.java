package pl.poznan.put.matching;

import pl.poznan.put.circular.Angle;
import pl.poznan.put.interfaces.Exportable;
import pl.poznan.put.interfaces.Tabular;
import pl.poznan.put.pdb.PdbResidueIdentifier;
import pl.poznan.put.pdb.analysis.MoleculeType;
import pl.poznan.put.pdb.analysis.PdbCompactFragment;
import pl.poznan.put.pdb.analysis.PdbResidue;
import pl.poznan.put.pdb.analysis.ResidueCollection;
import pl.poznan.put.protein.torsion.ProteinTorsionAngleType;
import pl.poznan.put.rna.torsion.RNATorsionAngleType;
import pl.poznan.put.torsion.MasterTorsionAngleType;
import pl.poznan.put.torsion.TorsionAngleValue;
import pl.poznan.put.types.ExportFormat;
import pl.poznan.put.utility.AngleFormat;
import pl.poznan.put.utility.TabularExporter;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class StructureSelection
        implements Exportable, Tabular, ResidueCollection {
    private static final int MINIMUM_RESIDUES_IN_A_COMPACT_FRAGMENT = 3;

    public static StructureSelection divideIntoCompactFragments(
            final String name, final Iterable<PdbResidue> residues) {
        final List<PdbResidue> candidates = new ArrayList<>();

        for (final PdbResidue residue : residues) {
            if (!residue.isMissing()) {
                candidates.add(residue);
            }
        }

        final List<PdbCompactFragment> compactFragments = new ArrayList<>();
        List<PdbResidue> currentFragmentResidues = new ArrayList<>();
        int index = 0;

        while (!candidates.isEmpty()) {
            final PdbResidue current = candidates.get(index);
            currentFragmentResidues.add(current);
            candidates.remove(index);
            index = current.findConnectedResidueIndex(candidates);

            if (index == -1) {
                if (currentFragmentResidues.size() >=
                    StructureSelection.MINIMUM_RESIDUES_IN_A_COMPACT_FRAGMENT) {
                    final String fragmentName = StructureSelection
                            .generateFragmentName(name,
                                                  currentFragmentResidues);
                    final PdbCompactFragment compactFragment =
                            new PdbCompactFragment(fragmentName,
                                                   currentFragmentResidues);
                    compactFragments.add(compactFragment);
                }

                currentFragmentResidues = new ArrayList<>();
                index = 0;
            }
        }

        return new StructureSelection(name, compactFragments);
    }

    private static String generateFragmentName(final String name,
                                               final List<PdbResidue>
                                                       fragmentResidues) {
        assert !fragmentResidues.isEmpty();

        if (fragmentResidues.size() == 1) {
            final PdbResidue residue = fragmentResidues.get(0);
            return String.format("%s %s.%d", name, residue.getChainIdentifier(),
                                 residue.getResidueNumber());
        }

        final PdbResidue first = fragmentResidues.get(0);
        final PdbResidue last =
                fragmentResidues.get(fragmentResidues.size() - 1);
        return String.format("%s %s.%d-%d", name, first.getChainIdentifier(),
                             first.getResidueNumber(), last.getResidueNumber());
    }

    private final List<PdbCompactFragment> compactFragments;

    private final String name;
    private final List<PdbResidue> residues;

    public StructureSelection(final String name,
                              final Collection<PdbCompactFragment>
                                      compactFragments) {
        super();
        this.name = name;
        this.compactFragments = new ArrayList<>(compactFragments);

        residues = new ArrayList<>();
        for (final PdbCompactFragment compactFragment : compactFragments) {
            residues.addAll(compactFragment.getResidues());
        }
    }

    public final String getName() {
        return name;
    }

    public final List<Angle> getValidTorsionAngleValues(
            final MasterTorsionAngleType masterType) {
        final List<Angle> angles = new ArrayList<>();

        for (final PdbCompactFragment fragment : compactFragments) {
            for (final PdbResidue residue : fragment.getResidues()) {
                final TorsionAngleValue angleValue =
                        fragment.getTorsionAngleValue(residue, masterType);
                final Angle angle = angleValue.getValue();
                if (angle.isValid()) {
                    angles.add(angle);
                }
            }
        }

        return angles;
    }

    @Override
    public final List<PdbResidue> getResidues() {
        return Collections.unmodifiableList(residues);
    }

    @Override
    public final PdbResidue findResidue(final String chainIdentifier,
                                        final int residueNumber,
                                        final String insertionCode) {
        return findResidue(
                new PdbResidueIdentifier(chainIdentifier, residueNumber,
                                         insertionCode));
    }

    @Override
    public final PdbResidue findResidue(final PdbResidueIdentifier query) {
        for (final PdbResidue residue : residues) {
            if (query.equals(residue.getResidueIdentifier())) {
                return residue;
            }
        }
        throw new IllegalArgumentException("Failed to find residue: " + query);
    }

    public final List<PdbCompactFragment> getCompactFragments() {
        return Collections.unmodifiableList(compactFragments);
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        final StructureSelection other = (StructureSelection) o;
        return Objects.equals(name, other.name) &&
               Objects.equals(residues, other.residues);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(name, residues);
    }

    @Override
    public final String toString() {
        final PdbResidue first = residues.get(0);
        final PdbResidue last = residues.get(residues.size() - 1);
        return String
                .format("%s - %s (count: %d)", first, last, residues.size());
    }

    @Override
    public final void export(final OutputStream outputStream)
            throws IOException {
        TabularExporter.export(asExportableTableModel(), outputStream);
    }

    @Override
    public final ExportFormat getExportFormat() {
        return ExportFormat.CSV;
    }

    @Override
    public final File suggestName() {
        return new File(name + ".csv");
    }

    @Override
    public final TableModel asExportableTableModel() {
        return asTableModel(false);
    }

    @Override
    public final TableModel asDisplayableTableModel() {
        return asTableModel(true);
    }

    private TableModel asTableModel(final boolean isDisplayable) {
        final Set<MasterTorsionAngleType> allAngleTypes =
                getCommonTorsionAngleTypes();
        final Collection<String> columns = new LinkedHashSet<>();
        columns.add("Residue");

        for (final MasterTorsionAngleType angleType : allAngleTypes) {
            columns.add(isDisplayable ? angleType.getLongDisplayName()
                                      : angleType.getExportName());
        }

        int rowCount = 0;

        for (final PdbCompactFragment fragment : compactFragments) {
            final List<PdbResidue> fragmentResidues = fragment.getResidues();
            rowCount += fragmentResidues.size();
        }

        final String[][] data = new String[rowCount][];
        int i = 0;

        for (final PdbCompactFragment fragment : compactFragments) {
            for (final PdbResidue residue : fragment.getResidues()) {
                final List<String> row = new ArrayList<>();
                row.add(residue.toString());

                for (final MasterTorsionAngleType angleType : allAngleTypes) {
                    final TorsionAngleValue angleValue =
                            fragment.getTorsionAngleValue(residue, angleType);
                    final double radians = angleValue.getValue().getRadians();
                    row.add(isDisplayable ? AngleFormat
                            .formatDisplayShort(radians)
                                          : AngleFormat.formatExport(radians));
                }

                data[i] = row.toArray(new String[row.size()]);
                i += 1;
            }
        }

        return new DefaultTableModel(data, columns.toArray(
                new String[columns.size()]));
    }

    public final Set<MasterTorsionAngleType> getCommonTorsionAngleTypes() {
        final Set<MasterTorsionAngleType> commonTypes = new LinkedHashSet<>();

        for (final PdbCompactFragment compactFragment : compactFragments) {
            final MoleculeType moleculeType = compactFragment.getMoleculeType();
            final Collection<? extends MasterTorsionAngleType> angleTypes;

            switch (moleculeType) {
                case PROTEIN:
                    angleTypes =
                            Arrays.asList(ProteinTorsionAngleType.values());
                    break;
                case RNA:
                    angleTypes = Arrays.asList(RNATorsionAngleType.values());
                    break;
                case UNKNOWN:
                default:
                    throw new IllegalArgumentException(
                            "Unknown molecule type: " + moleculeType);
            }

            commonTypes.addAll(angleTypes);
        }

        return commonTypes;
    }

    public final int size() {
        return residues.size();
    }
}

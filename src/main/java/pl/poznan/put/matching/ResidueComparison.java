package pl.poznan.put.matching;

import java.util.List;

import pl.poznan.put.pdb.analysis.PdbResidue;
import pl.poznan.put.torsion.TorsionAngleDelta;
import pl.poznan.put.torsion.type.MasterTorsionAngleType;
import pl.poznan.put.torsion.type.TorsionAngleType;

public class ResidueComparison {
    private final PdbResidue target;
    private final PdbResidue model;
    private final List<TorsionAngleDelta> angleDeltas;

    public ResidueComparison(PdbResidue target, PdbResidue model,
            List<TorsionAngleDelta> angleDeltas) {
        super();
        this.target = target;
        this.model = model;
        this.angleDeltas = angleDeltas;
    }

    public PdbResidue getTarget() {
        return target;
    }

    public PdbResidue getModel() {
        return model;
    }

    public TorsionAngleDelta getAngleDelta(MasterTorsionAngleType masterType) {
        for (TorsionAngleDelta delta : angleDeltas) {
            for (TorsionAngleType angleType : masterType.getAngleTypes()) {
                if (angleType.equals(delta.getMasterTorsionAngleType())) {
                    return delta;
                }
            }
        }
        return TorsionAngleDelta.bothInvalidInstance(masterType);
    }
}

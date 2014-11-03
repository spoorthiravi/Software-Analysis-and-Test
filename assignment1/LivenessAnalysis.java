import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.project.Chord;

@Chord(name = "liveness")
public class LivenessAnalysis extends DataflowAnalysis<Register> {

	static class QuadMetadata {
		Quad quad;
		private HashSet<Register> genSet;
		private HashSet<Register> killedSet;
		private HashSet<Register> inSet;
		private HashSet<Register> outSet;

		public QuadMetadata(Quad quad) {
			this.quad = quad;
			genSet = new HashSet<Register>();
			killedSet = new HashSet<Register>();
			inSet = new HashSet<Register>();
			outSet = new HashSet<Register>();
		}
	}

	@Override
	public void doAnalysis() {
		if (main.isAbstract())
			throw new RuntimeException("Method " + main + " is abstract");
		ControlFlowGraph cfg = main.getCFG();
		LinkedHashMap<Quad, QuadMetadata> linkedQuadMetaMap1 = new LinkedHashMap<Quad, QuadMetadata>();
		LinkedHashMap<Quad, QuadMetadata> linkedQuadMetaMap2 = new LinkedHashMap<Quad, QuadMetadata>();
		ArrayList<QuadMetadata> quadMetaList1 = new ArrayList<QuadMetadata>();
		ArrayList<QuadMetadata> quadMetaList2 = new ArrayList<QuadMetadata>();
		BasicBlock root = cfg.entry();
		HashMap<BasicBlock, ArrayList<QuadMetadata>> workListMap = new HashMap<BasicBlock, ArrayList<QuadMetadata>>();
		boolean updated = true;
		while (updated) {
			for (BasicBlock b : cfg.postOrderOnReverseGraph(root)) {
				for (int i = 0; i < b.size(); i++) {
					Quad q = b.getQuad(i);
					QuadMetadata quadMeta = new QuadMetadata(q);
					List<RegisterOperand> definedRegisterOperandList = q
							.getDefinedRegisters();
					List<RegisterOperand> usedRegisterOperandList = q
							.getUsedRegisters();
					HashSet<Register> genSet = new HashSet<Register>();
					HashSet<Register> killedSet = new HashSet<Register>();
					for (RegisterOperand regOp : usedRegisterOperandList) {
						genSet.add(regOp.getRegister());
					}
					for (RegisterOperand regOp : definedRegisterOperandList) {
						killedSet.add(regOp.getRegister());
					}

					// get kill and gen set for each quad
					quadMeta.killedSet = killedSet;
					quadMeta.genSet = genSet;

					// calculate ouSet for each quad
					if (b.isEntry() && i == 0
							&& b.getNumberOfPredecessors() == 0) {
						quadMeta.outSet.add(null);
					} else if (!b.isEntry() && i == 0) {
						ArrayList<BasicBlock> predecessorsList = (ArrayList<BasicBlock>) b
								.getPredecessors();
						for (BasicBlock predecessor : predecessorsList) {
							ArrayList<QuadMetadata> workListValues = workListMap
									.get(predecessor);
							quadMeta.outSet.addAll((workListValues
									.get(workListValues.size() - 1)).inSet);
						}
					} else {
						if (linkedQuadMetaMap2.containsKey(q)) {
							quadMeta.outSet
									.addAll(quadMetaList2.get(i - 1).inSet);
						} else {
							quadMeta.outSet
									.addAll(quadMetaList1.get(i - 1).inSet);
						}

					}

					// calculate inSet for each quad
					HashSet<Register> dupOutSet = quadMeta.outSet;
					for (Register register : quadMeta.killedSet)
						dupOutSet.remove(register);
					dupOutSet.addAll(quadMeta.genSet);
					quadMeta.inSet = dupOutSet;
					if (linkedQuadMetaMap1.containsKey(q)) {
						quadMetaList2.add(quadMeta);
						linkedQuadMetaMap2.put(q, quadMeta);
					} else {
						quadMetaList1.add(quadMeta);
						linkedQuadMetaMap1.put(q, quadMeta);
					}
				}

			}
			boolean flag = true;
			for (Entry<Quad, QuadMetadata> entry : linkedQuadMetaMap1
					.entrySet()) {
				Quad quad = entry.getKey();
				if (linkedQuadMetaMap2.containsKey(quad)) {
					if (((entry.getValue().inSet).equals(linkedQuadMetaMap2
							.get(quad).inSet))
							&& ((entry.getValue().outSet)
									.equals(linkedQuadMetaMap2.get(quad).outSet))) {
						flag = flag && true;
					} else {
						flag = false;
					}
				}
			}
			if (flag) {
				updated = false;
				for (QuadMetadata quadMeta : quadMetaList1) {
					inMap.put(quadMeta.quad, quadMeta.inSet);
					outMap.put(quadMeta.quad, quadMeta.outSet);
				}

			} else {
				quadMetaList1 = (ArrayList<QuadMetadata>) quadMetaList2.clone();
				quadMetaList2 = new ArrayList<QuadMetadata>();
				linkedQuadMetaMap1 = new LinkedHashMap<Quad, QuadMetadata>(
						linkedQuadMetaMap2);
				linkedQuadMetaMap2 = new LinkedHashMap<Quad, QuadMetadata>();
			}

		}

	}

}

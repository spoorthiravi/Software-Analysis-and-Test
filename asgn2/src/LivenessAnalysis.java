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
		System.out.println("HELLO I AM IN DO ANALYSIS");
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
		int count = 0;
		while (count < 50) {
			for (BasicBlock b : cfg.postOrderOnReverseGraph(root)) {
				ArrayList<QuadMetadata> workListMapValues = new ArrayList<QuadMetadata>(); 
				for (int i = b.size()-1; i >= 0; i--) {
					Quad q = b.getQuad(i);
					QuadMetadata quadMeta = new QuadMetadata(q);
					List<RegisterOperand> definedRegisterOperandList = q.getDefinedRegisters();
					List<RegisterOperand> usedRegisterOperandList = q.getUsedRegisters();
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
					if (b.isEntry() && i == b.size()-1 && b.getNumberOfSuccessors() == 0) {
						quadMeta.outSet = new HashSet<Register>();
					} else if (!b.isEntry() && i == b.size()-1) {
						ArrayList<BasicBlock> successorsList = (ArrayList<BasicBlock>) b.getSuccessors();
						for (BasicBlock successor : successorsList) {
							if(workListMap.containsKey(successor)){
							ArrayList<QuadMetadata> workListValues = workListMap.get(successor);
							quadMeta.outSet.addAll((workListValues.get(workListValues.size() - 1)).inSet);
							}else{
								System.out.println("successor not added to map yet");
							}
						}
					} else {
						if (linkedQuadMetaMap2.containsKey(q)) {
							int size2 = quadMetaList2.size();
							quadMeta.outSet.addAll(quadMetaList2.get(size2 - 1).inSet);
						} else {
							int size1 = quadMetaList1.size();
							quadMeta.outSet.addAll(quadMetaList1.get(size1 - 1).inSet);
						}

					}

					// calculate inSet for each quad
					HashSet<Register> dupOutSet = quadMeta.outSet;
					for (Register register : quadMeta.killedSet)
						dupOutSet.remove(register);
					dupOutSet.addAll(quadMeta.genSet);
					quadMeta.inSet = dupOutSet;
					
					//adding quadetadata to map and list
					if (linkedQuadMetaMap1.containsKey(q)) {
						quadMetaList2.add(quadMeta);
						linkedQuadMetaMap2.put(q, quadMeta);
					} else {
						quadMetaList1.add(quadMeta);
						linkedQuadMetaMap1.put(q, quadMeta);
					}
					workListMapValues.add(quadMeta);
					System.out.println("quadMetaList1 =" + quadMetaList1);
					System.out.println("quadMetaList2 =" + quadMetaList2);
				}
				if(workListMap.containsKey(b)){
					workListMap.remove(b);
					workListMap.put(b, workListMapValues);
				}else{
					workListMap.put(b, workListMapValues);
				}

			}
			boolean flag = true;
			for (Entry<Quad, QuadMetadata> entry : linkedQuadMetaMap1.entrySet()) {
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
				flag = false;
			}
			if (count == 49) {
				updated = false;
				for (QuadMetadata quadMeta : quadMetaList1) {
					inMap.put(quadMeta.quad, quadMeta.inSet);
					outMap.put(quadMeta.quad, quadMeta.outSet);
					System.out.println("inMap size = " + inMap.size());
					System.out.println("outMap size = " + outMap.size());
				}

			} else {
				quadMetaList1 = (ArrayList<QuadMetadata>) quadMetaList2.clone();
				quadMetaList2 = new ArrayList<QuadMetadata>();
				linkedQuadMetaMap1 = new LinkedHashMap<Quad, QuadMetadata>(linkedQuadMetaMap2);
				linkedQuadMetaMap2 = new LinkedHashMap<Quad, QuadMetadata>();
			}
			count++;

		}

	}

}

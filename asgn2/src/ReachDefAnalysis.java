import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.project.Chord;
import chord.util.tuple.object.Pair;

@Chord(name = "reachdef")
public class ReachDefAnalysis extends DataflowAnalysis<Pair<Quad, Register>> {

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
//		System.out.println("number of basic blocks = " + cfg.getNumberOfBasicBlocks());
//		System.out.println("predecesors of entry block = " + cfg.entry().getNumberOfPredecessors());
//		System.out.println("index of entry block = " + cfg.entry().getID());
		
		LinkedHashMap<Quad, QuadMetadata> linkedQuadMetaMap1 = new LinkedHashMap<Quad, QuadMetadata>();
		LinkedHashMap<Quad, QuadMetadata> linkedQuadMetaMap2 = new LinkedHashMap<Quad, QuadMetadata>();
		ArrayList<QuadMetadata> quadMetaList1 = new ArrayList<QuadMetadata>();
		ArrayList<QuadMetadata> quadMetaList2 = new ArrayList<QuadMetadata>();
		HashMap<BasicBlock, ArrayList<QuadMetadata>> workListMap = new HashMap<BasicBlock, ArrayList<QuadMetadata>>();
		
		boolean updated = true;
		int count = 0;
		while (count < 50) {
			for (BasicBlock b : cfg.reversePostOrder()) {
//				System.out.println("block id = " + b.getID());
//				System.out.println("num of quads each blk has = " + b.getQuads().size());
				/*if(!b.isEntry()){
					for(BasicBlock predecessor : b.getPredecessors()){
						System.out.println("numOFpredecessors , predecessor = " + b.getNumberOfPredecessors() + "," + predecessor.getID());
					}
				}*/
				ArrayList<QuadMetadata> workListMapValues = new ArrayList<QuadMetadata>();
				for (int i = 0; i < b.size(); i++) {
					Quad q = b.getQuad(i);
					QuadMetadata quadMeta = new QuadMetadata(q);

					/**
					 * I want to put the inSet and outSet for each Quad in the
					 * induvidaualQuadMetaList so that at the end of every loop
					 * I can check if the if the inSet for each Quad has changed
					 * if it hasn't changed then I set updated to false so loop
					 * will break and then for each Quad I will take the last
					 * changed inSet and ouSet and put it in inMap and outMap
					 * respectively. so I need only one induvidualQuadMetaList
					 * so I will check if its null and only if its null I'll
					 * create it
					 */

					List<RegisterOperand> definedRegisterOperandsList = new ArrayList<RegisterOperand>();
					definedRegisterOperandsList.addAll(q.getDefinedRegisters());
					
					HashSet<Register> genSet = new HashSet<Register>();
					HashSet<Register> killedSet = new HashSet<Register>();
					//populating genSet
					for (RegisterOperand regOp : definedRegisterOperandsList) {
						genSet.add(regOp.getRegister());
					}
					
					
					// killed Set for each quad; what if there is a loop to first quad
	//				System.out.println("i = " + i);
					if (i == 0 && b.isEntry() && b.getNumberOfPredecessors() == 0) {
	//					System.out.println("Enters the first if b = Entry");
	//					killedSet.addAll(genSet);
						quadMeta.killedSet = killedSet;
					}else if (!b.isEntry() && i == 0) {
						ArrayList<BasicBlock> predecessorsList = (ArrayList<BasicBlock>) b.getPredecessors();
	//					System.out.println("enters the else if !b.isEntry and i = 0");
						
						for(Register register: genSet){
							for (BasicBlock predecessor : predecessorsList) {
								if(predecessorsList.size() == 1 && predecessor.getQuads().size() == 0 && predecessorsList.get(0).isEntry()){
									break;
								}
								if(predecessor.getQuads().size() == 0)
									break;
								try{
								ArrayList<QuadMetadata> workListValues = workListMap.get(predecessor);
								QuadMetadata qMetadata =  workListValues.get(workListValues.size()-1);
									if(qMetadata.genSet.contains(register)){
										killedSet.add(register);
									}
								}catch(Exception ex){
									killedSet = new HashSet<Register>();
								}
							}
						}
	//					killedSet.addAll(genSet);
						quadMeta.killedSet = killedSet;	
					}else {
						for (Register register : genSet) {
	//						System.out.println("enters the else:100");
							// should u remove all the registers in the genSet of previous quad that were generated in theenSet of this quad
							if (!linkedQuadMetaMap2.isEmpty()) {
								int size = quadMetaList2.size();
								if (quadMetaList2.get(size - 1).genSet.contains(register)) {
									killedSet.add(register);
								}
							} else {
								int size = quadMetaList1.size();
								if (quadMetaList1.get(size-1).genSet.contains(register)) {
									killedSet.add(register);
								}
							}
						}
	//					killedSet.addAll(genSet);
						quadMeta.killedSet = killedSet;
					}
					// get kill and gen set for each quad
					quadMeta.genSet = genSet;
					
					// calculate inSet for each quad
					if (b.isEntry() && i == 0) {
						for (RegisterOperand regOp : q.getDefinedRegisters())
							quadMeta.inSet.add(regOp.getRegister());
					} else if (!b.isEntry() && i == 0) {
						ArrayList<BasicBlock> predecessorsList = (ArrayList<BasicBlock>) b.getPredecessors();
						for (BasicBlock predecessor : predecessorsList) {
							if(predecessorsList.size() == 1 && predecessor.getQuads().size() == 0 && predecessorsList.get(0).isEntry()){
								quadMeta.inSet = new HashSet<Register>();
								break;
							}
							if(predecessor.getQuads().size() == 0){
								quadMeta.inSet = new HashSet<Register>();
								break;
							}
							if(workListMap.containsKey(predecessor)){
								ArrayList<QuadMetadata> workListValues = workListMap.get(predecessor);
								quadMeta.inSet.addAll((workListValues.get(workListValues.size() - 1)).outSet);
							}else{
	//							System.out.println("predecessor not processed yet");
							}
						}
					} else {
						if(!quadMetaList2.isEmpty()){
							int size2 = quadMetaList2.size();
							quadMeta.inSet.addAll(quadMetaList2.get(size2-1).outSet);
						}else{
							int size1 = quadMetaList1.size();
							quadMeta.inSet.addAll(quadMetaList1.get(size1-1).outSet);
						}
					}

					// calculate outSet for each quad
					HashSet<Register> dupOutSet = quadMeta.inSet;
					
					for (Register register : quadMeta.killedSet){
						dupOutSet.remove(register);
					}
						dupOutSet.addAll(quadMeta.genSet);
						
					quadMeta.outSet = dupOutSet;
					
					if (linkedQuadMetaMap1.containsKey(q)) {
						quadMetaList2.add(quadMeta);
						linkedQuadMetaMap2.put(q, quadMeta);
					} else {
						quadMetaList1.add(quadMeta);
						linkedQuadMetaMap1.put(q, quadMeta);
					}
					
					/*for(QuadMetadata qMeta : workListMapValues){
						if(qMeta.quad == q){
							workListMapValues.remove(qMeta);
						}
					}*/
					workListMapValues.add(quadMeta);
	//				System.out.println("quadMetaList1 =" + quadMetaList1);
	//				System.out.println("quadMetaList2 =" + quadMetaList2);

				}
				if(workListMap.containsKey(b)){
					workListMap.remove(b);
					workListMap.put(b, workListMapValues);
				}else{
					workListMap.put(b, workListMapValues);
				}
	//			System.out.println("workListMapValues Size = " + workListMapValues.size());
	//			System.out.println("WorkListMap Size = " + workListMap.size());
			}
			boolean flag = true;
			int i = 0;
			for (Entry<Quad, QuadMetadata> entry : linkedQuadMetaMap1.entrySet()) {
				Quad quad = entry.getKey();
				if (linkedQuadMetaMap2.containsKey(quad)) {
					if ( ((entry.getValue().inSet).equals(linkedQuadMetaMap2.get(quad).inSet))
							&& ((entry.getValue().outSet).equals(linkedQuadMetaMap2.get(quad).outSet)) ) {
						flag = flag && true;
						i++;
					} else {
						flag = false;
					}
				}
				flag = false;
			}
	//		System.out.println("number of matches = " + i);
			if (count == 49) {
	//			System.out.println("REACHED FLAG = TRUE!!!!!! IT TERMINATES!!!!!!");
				updated = false;
				for (QuadMetadata quadMeta : quadMetaList1) {
					Set<Pair<Quad, Register>> inPairSet = new HashSet<Pair<Quad, Register>>();
					HashSet<Pair<Quad, Register>> outPairSet = new HashSet<Pair<Quad, Register>>();
					for (Register register : quadMeta.inSet) {
						Pair<Quad, Register> pair = new Pair<Quad, Register>(quadMeta.quad, register);
						inPairSet.add(pair);
					}
					inMap.put(quadMeta.quad, inPairSet);
					for (Register register : quadMeta.outSet) {
						Pair<Quad, Register> pair = new Pair<Quad, Register>(quadMeta.quad, register);
						outPairSet.add(pair);
					}
					outMap.put(quadMeta.quad, outPairSet);
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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
	
	@Override
	public void doAnalysis() {
		boolean updated = true;
		int whileLoopCount = 0;
		initialize();
		while(updated){
			updated = getLiveVariables(whileLoopCount);
			whileLoopCount++;
		}
		
	}

	private void initialize(){
		if (main.isAbstract())throw new RuntimeException("Method " + main + " is abstract");
		ControlFlowGraph cfg = main.getCFG();
		for (BasicBlock b : cfg.reversePostOrder()) {
			for (int i = 0; i < b.size(); i++) {
				Quad q = b.getQuad(i);
				HashSet<Pair<Quad, Register>> inPairSet = new HashSet<Pair<Quad, Register>>();
				HashSet<Pair<Quad, Register>> outPairSet = new HashSet<Pair<Quad, Register>>();
				inMap.put(q,inPairSet);
				outMap.put(q, outPairSet);
			}
		}
		
	}
	
	
	private boolean getLiveVariables(int whileLoopCount) {
		if (main.isAbstract())throw new RuntimeException("Method " + main + " is abstract");
		ControlFlowGraph cfg = main.getCFG();
		boolean flag = true;
		for (BasicBlock b : cfg.reversePostOrder()) {
			for (int i = 0; i < b.size(); i++) {
				Quad q = b.getQuad(i);
				HashSet<Pair<Quad, Register>> tempInPairSet = new HashSet<Pair<Quad, Register>>();
				HashSet<Pair<Quad, Register>> tempOutPairSet = new HashSet<Pair<Quad, Register>>();
				HashSet<Pair<Quad, Register>> killedSet = new HashSet<Pair<Quad, Register>>();
				HashSet<Pair<Quad, Register>> genSet = new HashSet<Pair<Quad, Register>>();
				if(whileLoopCount < 1){
					if(i == 0){
						ArrayList<BasicBlock> predecessorsList = (ArrayList<BasicBlock>) b.getPredecessors();
						for (BasicBlock predecessor : predecessorsList) {
							if(predecessor.size() > 0){
								tempInPairSet.addAll(outMap.get(predecessor.getQuad(predecessor.size()-1)));
							}
						}
						killedSet.addAll(getKilledSet(q,tempInPairSet));
						genSet.addAll(getGenSet(q));
						tempOutPairSet.addAll(tempInPairSet);
						tempOutPairSet.removeAll(killedSet);
						tempOutPairSet.addAll(genSet);
						outMap.get(q).addAll(tempOutPairSet);
						inMap.get(q).addAll(tempInPairSet);
						
					}else{
						tempInPairSet.addAll(outMap.get(b.getQuad(i-1)));
						killedSet.addAll(getKilledSet(q,tempInPairSet));
						genSet.addAll(getGenSet(q));
						tempOutPairSet.addAll(tempInPairSet);
						tempOutPairSet.removeAll(killedSet);
						tempOutPairSet.addAll(genSet);
						outMap.get(q).addAll(tempOutPairSet);
						inMap.get(q).addAll(tempInPairSet);
					}
					flag = false;
					
				}
				else{
					if(i == 0){
						ArrayList<BasicBlock> predecessorsList = (ArrayList<BasicBlock>) b.getPredecessors();
						for (BasicBlock predecessor : predecessorsList) {
							if(predecessor.size() > 0){
								tempInPairSet.addAll(outMap.get(predecessor.getQuad(predecessor.size()-1)));
							}
						}
						killedSet.addAll(getKilledSet(q,tempInPairSet));
						genSet.addAll(getGenSet(q));
						tempOutPairSet.addAll(tempInPairSet);
						tempOutPairSet.removeAll(killedSet);
						tempOutPairSet.addAll(genSet);
						if(isSame(tempOutPairSet,outMap.get(q))){
							tempOutPairSet.removeAll(tempOutPairSet);
							flag = flag && true;
						}else{
							outMap.get(q).clear();
							outMap.get(q).addAll(tempOutPairSet);
							flag = false;
						}
						if(isSame(tempInPairSet,inMap.get(q))){
							tempInPairSet.removeAll(tempInPairSet);
							flag = flag && true;
							
						}else{
							inMap.get(q).clear();
							inMap.get(q).addAll(tempInPairSet);
							flag = false;	
						}
						
					}else{
						tempInPairSet.addAll(outMap.get(b.getQuad(i-1)));
						killedSet.addAll(getKilledSet(q,tempInPairSet));
						genSet.addAll(getGenSet(q));
						tempOutPairSet.addAll(tempInPairSet);
						tempOutPairSet.removeAll(killedSet);
						tempOutPairSet.addAll(genSet);
						if(isSame(tempOutPairSet,outMap.get(q))){
							tempOutPairSet.removeAll(tempOutPairSet);
							flag = flag && true;
						}else{
							outMap.get(q).clear();
							outMap.get(q).addAll(tempOutPairSet);
							flag = false;
						}
						if(isSame(tempInPairSet,inMap.get(q))){
							tempInPairSet.removeAll(tempInPairSet);
							flag = flag && true;
							
						}else{
							inMap.get(q).clear();
							inMap.get(q).addAll(tempInPairSet);
							flag = false;	
						}
						
					}
					
				}
				
				
			}
		
		}
		if(flag)
			return false;
		else
			return true;
	}

	private boolean isSame(HashSet<Pair<Quad,Register>> set1, Set<Pair<Quad,Register>> set2) {
		if(set1.size() != set2.size()){
			return false;
		}
		for(Pair<Quad,Register> p : set1){
			if(!set2.contains(p))
				return false;
		}
		return true;
	}

	private HashSet<Pair<Quad,Register>> getKilledSet(Quad q, HashSet<Pair<Quad, Register>> inSet) {
		HashSet<Pair<Quad,Register>> killedSet = new HashSet<Pair<Quad,Register>>();
		List<RegisterOperand> definedRegisterOperandList = q.getDefinedRegisters();
		for(Pair<Quad,Register> pair : inSet){
			for (RegisterOperand regOp : definedRegisterOperandList) {
				if(pair.val1 == regOp.getRegister()){
					killedSet.add(pair);
				}
			}
		}
		return killedSet;
	}

	private HashSet<Pair<Quad,Register>> getGenSet(Quad q) {
		HashSet<Pair<Quad,Register>> genSet = new HashSet<Pair<Quad,Register>>();
		List<RegisterOperand> definedRegisterOperandList = q.getDefinedRegisters();
		for (RegisterOperand regOp : definedRegisterOperandList) {
			Pair<Quad,Register> p = new Pair<Quad,Register>(q, regOp.getRegister());
			genSet.add(p);
		}
		return genSet;
	}
	

}

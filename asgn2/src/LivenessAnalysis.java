import joeq.Compiler.Quad.RegisterFactory.Register;
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

@Chord(name = "liveness")
public class LivenessAnalysis extends DataflowAnalysis<Register>{

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
		for (BasicBlock b : cfg.reversePostOrderOnReverseGraph()) {
			for (int i = b.size()-1; i >= 0; i--) {
				Quad q = b.getQuad(i);
				HashSet<Register> inSet = new HashSet<Register>();
				HashSet<Register> outSet = new HashSet<Register>();
				inMap.put(q,inSet);
				outMap.put(q, outSet);
			}
		}
		
	}
	
	
	private boolean getLiveVariables(int whileLoopCount) {
		if (main.isAbstract())
			throw new RuntimeException("Method " + main + " is abstract");
		ControlFlowGraph cfg = main.getCFG();
		boolean flag = true;
		for (BasicBlock b : cfg.reversePostOrderOnReverseGraph()) {
			for (int i = b.size()-1; i >= 0; i--) {
				Quad q = b.getQuad(i);
				HashSet<Register> tempInSet = new HashSet<Register>(); 
				HashSet<Register> tempOutSet = new HashSet<Register>();
				HashSet<Register> killedSet = new HashSet<Register>();
				HashSet<Register> genSet = new HashSet<Register>();
				if(whileLoopCount < 1){
					if(i == b.size()-1){
						ArrayList<BasicBlock> successorsList = (ArrayList<BasicBlock>) b.getSuccessors();
						for (BasicBlock successor : successorsList) {
							if(successor.size() > 0){
								tempOutSet.addAll(inMap.get(successor.getQuad(0)));
							}
						}
						killedSet.addAll(getKilledSet(q));
						genSet.addAll(getGenSet(q));
						tempInSet.addAll(tempOutSet);
						tempInSet.removeAll(killedSet);
						tempInSet.addAll(genSet);
						outMap.get(q).addAll(tempOutSet);
						inMap.get(q).addAll(tempInSet);
						
					}else{
						tempOutSet.addAll((HashSet<Register>) inMap.get(b.getQuad(i+1)));
						killedSet.addAll(getKilledSet(q));
						genSet.addAll(getGenSet(q));
						tempInSet.addAll(tempOutSet);
						tempInSet.removeAll(killedSet);
						tempInSet.addAll(genSet);
						outMap.get(q).addAll(tempOutSet);
						inMap.get(q).addAll(tempInSet);
					}
					flag = false;
					
				}
				else{
					if(i == b.size()-1){
						ArrayList<BasicBlock> successorsList = (ArrayList<BasicBlock>) b.getSuccessors();
						for (BasicBlock successor : successorsList) {
							if(successor.size() > 0){
								tempOutSet.addAll(inMap.get(successor.getQuad(0)));
							}
						}
						killedSet.addAll(getKilledSet(q));
						genSet.addAll(getGenSet(q));
						tempInSet.addAll(tempOutSet);
						tempInSet.removeAll(killedSet);
						tempInSet.addAll(genSet);
						if(isSame(tempOutSet,outMap.get(q))){
							tempOutSet.removeAll(tempOutSet);
							flag = flag && true;
						}else{
							outMap.get(q).clear();
							outMap.get(q).addAll(tempOutSet);
							flag = false;
						}
						if(isSame(tempInSet,inMap.get(q))){
							tempInSet.removeAll(tempInSet);
							flag = flag && true;
							
						}else{
							inMap.get(q).clear();
							inMap.get(q).addAll(tempInSet);
							flag = false;	
						}
						
					}else{
						tempOutSet.addAll((HashSet<Register>) inMap.get(b.getQuad(i+1)));
						killedSet.addAll(getKilledSet(q));
						genSet.addAll(getGenSet(q));
						tempInSet.addAll(tempOutSet);
						tempInSet.removeAll(killedSet);
						tempInSet.addAll(genSet);
						if(isSame(tempOutSet,outMap.get(q))){
							tempOutSet.removeAll(tempOutSet);
							flag = flag && true;
						}else{
							outMap.get(q).clear();
							outMap.get(q).addAll(tempOutSet);
							flag = false;
						}
						if(isSame(tempInSet,inMap.get(q))){
							tempInSet.removeAll(tempInSet);
							flag = flag && true;
							
						}else{
							inMap.get(q).clear();
							inMap.get(q).addAll(tempInSet);
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

	private boolean isSame(HashSet<Register> set1, Set<Register> set2) {
		if(set1.size() != set2.size()){
			return false;
		}
		for(Register register : set1){
			if(!set2.contains(register))
				return false;
		}
		return true;
	}

	private HashSet<Register> getKilledSet(Quad q) {
		HashSet<Register> killedSet = new HashSet<Register>();
		List<RegisterOperand> definedRegisterOperandList = q.getDefinedRegisters();
		for (RegisterOperand regOp : definedRegisterOperandList) {
			killedSet.add(regOp.getRegister());
		}
		return killedSet;
	}

	private HashSet<Register> getGenSet(Quad q) {
		HashSet<Register> genSet = new HashSet<Register>();
		List<RegisterOperand> usedRegisterOperandList = q.getUsedRegisters();
		for (RegisterOperand regOp : usedRegisterOperandList) {
			genSet.add(regOp.getRegister());
		}
		return genSet;
	}

}

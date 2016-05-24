package notabot.propnet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Constant;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;

public class NotABotPropNetStateMachine extends StateMachine{
    /** The underlying proposition network  */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;

    // mapping from role to relevant moves
    private List<Map<Role, Set<Move>>> relevantInputMap;

    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override
    public void initialize(List<Gdl> description) {
        try {
			propNet = OptimizingPropNetFactory.create(description);
	        roles = propNet.getRoles();
	        ordering = getOrdering();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

        for (Role r: getRoles()){
        	System.out.println(r);
        	for (Proposition p: propNet.getLegalPropositions().get(r)){
        		System.out.println(p);
        	}
        	System.out.println();
        }

        relevantInputMap = findRelevantMoves();
    }

    public synchronized Set<Move> getRelevantSubgameMoves(int subgameIndex, Role role){
    	return relevantInputMap.get(subgameIndex).get(role);
    }

    public synchronized Map<Role, Set<Move>> getRelevantSubgameMoves(int subgameIndex){
    	return relevantInputMap.get(subgameIndex);
    }


    public synchronized int getNumSubgames(){
    	return relevantInputMap.size();
    }

	/**
	 * Computes if the state is terminal. Should return the value
	 * of the terminal proposition for the state.
	 */
	@Override
	public synchronized boolean isTerminal(MachineState state) {
		// Done
		setPropNetState(state);
		return propMark(propNet.getTerminalProposition());
	}

	/**
	 * Computes the goal for a role in the current state.
	 * Should return the value of the goal proposition that
	 * is true for that role. If there is not exactly one goal
	 * proposition true for that role, then you should throw a
	 * GoalDefinitionException because the goal is ill-defined.
	 */
	@Override
	public synchronized int getGoal(MachineState state, Role role)
	throws GoalDefinitionException {
		// Done
		setPropNetState(state);
		for (Proposition p: propNet.getGoalPropositions().get(role)){
			if (propMark(p)){
				return Integer.parseInt(p.getName().getBody().get(1).toString());
			}
		}
		return 0;
	}

	/**
	 * Returns the initial state. The initial state can be computed
	 * by only setting the truth value of the INIT proposition to true,
	 * and then computing the resulting state.
	 */
	@Override
	public synchronized MachineState getInitialState() {
		// Done
		clearPropNet();
		propNet.getInitProposition().setValue(true);
		return computeNextState();
	}

	/**
	 * Computes the legal moves for role in state.
	 */
	@Override
	public synchronized List<Move> getLegalMoves(MachineState state, Role role)
	throws MoveDefinitionException {
		// Done
		setPropNetState(state);
		List<Move> moves = new ArrayList<Move>();

		for (Proposition p: propNet.getLegalPropositions().get(role)){
			if (propMark(p)){
				moves.add(new Move(p.getName().getBody().get(1)));
			}
		}

		return moves;
	}

	/**
	 * Computes the next state given state and the list of moves.
	 */
	@Override
	public synchronized MachineState getNextState(MachineState state, List<Move> moves)
	throws TransitionDefinitionException {
		// Done
		setPropNetState(state, moves);
		return computeNextState();
	}



	/**
	 * This should compute the topological ordering of propositions.
	 * Each component is either a proposition, logical gate, or transition.
	 * Logical gates and transitions only have propositions as inputs.
	 *
	 * The base propositions and input propositions should always be exempt
	 * from this ordering.
	 *
	 * The base propositions values are set from the MachineState that
	 * operations are performed on and the input propositions are set from
	 * the Moves that operations are performed on as well (if any).
	 *
	 * @return The order in which the truth values of propositions need to be set.
	 */
	public synchronized List<Proposition> getOrdering()
	{
	    // List to contain the topological ordering.
	    List<Proposition> order = new LinkedList<Proposition>();

		// All of the components in the PropNet
		List<Component> components = new ArrayList<Component>(propNet.getComponents());

		// All of the propositions in the PropNet.
		List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());

	    // TODO: Compute the topological ordering.

		return order;
	}

	/* Already implemented for you */
	@Override
	public synchronized List<Role> getRoles() {
		return roles;
	}

	/* Helper methods */

	/**
	 * The Input propositions are indexed by (does ?player ?action).
	 *
	 * This translates a list of Moves (backed by a sentence that is simply ?action)
	 * into GdlSentences that can be used to get Propositions from inputPropositions.
	 * and accordingly set their values etc.  This is a naive implementation when coupled with
	 * setting input values, feel free to change this for a more efficient implementation.
	 *
	 * @param moves
	 * @return
	 */
	private List<GdlSentence> toDoes(List<Move> moves)
	{
		List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
		Map<Role, Integer> roleIndices = getRoleIndices();

		for (int i = 0; i < roles.size(); i++)
		{
			int index = roleIndices.get(roles.get(i));
			doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
		}
		return doeses;
	}

	/**
	 * Takes in a Legal Proposition and returns the appropriate corresponding Move
	 * @param p
	 * @return a PropNetMove
	 */
	public synchronized static Move getMoveFromProposition(Proposition p)
	{
		return new Move(p.getName().get(1));
	}

	/**
	 * Helper method for parsing the value of a goal proposition
	 * @param goalProposition
	 * @return the integer value of the goal proposition
	 */
    private int getGoalValue(Proposition goalProposition)
	{
		GdlRelation relation = (GdlRelation) goalProposition.getName();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}

	/**
	 * A Naive implementation that computes a PropNetMachineState
	 * from the true BasePropositions.  This is correct but slower than more advanced implementations
	 * You need not use this method!
	 * @return PropNetMachineState
	 */
	public synchronized MachineState getStateFromBase()
	{
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (Proposition p : propNet.getBasePropositions().values())
		{
			p.setValue(p.getSingleInput().getValue());
			if (p.getValue())
			{
				contents.add(p.getName());
			}

		}
		return new MachineState(contents);
	}

	private void setPropNetState(MachineState state){
		clearPropNet();
		markBases(state.getContents());
	}

	private void setPropNetState(MachineState state, List<Move> moves){
		setPropNetState(state);
		markActions(moves);
	}

	private void clearPropNet(){
		for (Proposition p: propNet.getBasePropositions().values()){
			p.setValue(false);
		}
		// TODO clearing inputs might not be necessary
		for (Proposition p: propNet.getInputPropositions().values()){p.setValue(false);}
		propNet.getInitProposition().setValue(false);
	}

	private void markBases(Set<GdlSentence> sentences){
		for (GdlSentence sent: sentences){
			propNet.getBasePropositions().get(sent).setValue(true);
		}
	}

	private void markActions(List<Move> moves){

		for (int i=0; i<getRoles().size(); i++){
			Move m = moves.get(i);
			if (m != null){
				for (Proposition legal: propNet.getLegalPropositions().get(getRoles().get(i))){
					if (legal.getName().get(1).equals(m.getContents())){
						Proposition does = propNet.getLegalInputMap().get(legal);
						propNet.getInputPropositions().get(does.getName()).setValue(true);
					}
				}
			}
		}

	}

	private boolean propMark(Component c){
		if (c instanceof Proposition){
			// view proposition
			// System.out.println(((Proposition) c).getName().getName());
			if (c.getInputs().size() == 1 && !(c.getSingleInput() instanceof Transition)){
				return propMark(c.getSingleInput());
			}
			// base or input proposition
			else{
				return c.getValue();
			}
		}
		else if (c instanceof And){
			for (Component c2:c.getInputs()){
				if (!propMark(c2)) return false;
			}
			return true;
		}
		else if (c instanceof Or){
			for (Component c2:c.getInputs()){
				if (propMark(c2)) return true;
			}
			return false;
		}
		else if (c instanceof Constant){
			//System.out.println("NotABotPropNetStateMachine found Constant during propMark");
			return c.getValue();
		}
		else if (c instanceof Not){
			return !propMark(c.getSingleInput());
		}
		else if (c instanceof Transition){
			return propMark(c.getSingleInput());
		}

		System.out.println("NotABotPropNetStateMachine found unknown Component during propMark");
		return false;
	}

	private MachineState computeCurrentState(){
		Set<GdlSentence> contents = new HashSet<GdlSentence>();

		for (GdlSentence s: propNet.getBasePropositions().keySet()){
			if (propNet.getBasePropositions().get(s).getValue()){
				contents.add(s);
			}
		}

		return new MachineState(contents);
	}


	private MachineState computeNextState(){
		int n = propNet.getBasePropositions().values().size();
		Proposition[] bases = propNet.getBasePropositions().values().toArray(new Proposition[n]);
		boolean[] vals = new boolean[bases.length];


		for (int i=0; i<bases.length; i++){
			vals[i] = propMark(bases[i].getSingleInput());
		}
		for (int i=0; i<bases.length; i++){
			bases[i].setValue(vals[i]);
		}

		return computeCurrentState();
	}

	/**
	 * Runs a reverse DFS on the propnet graph, starting from a given start component.
	 * Returns a set of all input propositions that can affect the value of the start component.
	 */
	private Set<Proposition> findSubgameInputs(Component start){
		// keeps track of relevant input propositions
		Set<Proposition> relevantInputs = new HashSet<Proposition>();
		// keeps track of visited components to prevent duplicates
		Set<Component> visited = new HashSet<Component>();
		// DFS stack
		Stack<Component> stack = new Stack<Component>();

		// initialize stack with starting proposition
		stack.add(start);
		visited.add(start);

		// run DFS on propnet
		while (!stack.isEmpty()){
			Component curr = stack.pop();
			for (Component comp: curr.getInputs()){
				// add unvisited components to stack
				if (!visited.contains(comp)){
					stack.add(comp);
					visited.add(comp);

					// add input propositions to set
					if (comp instanceof Proposition && comp.getInputs().size()==0){
						relevantInputs.add((Proposition) comp);
					}
				}
			}
		}

		// removes init proposition from list before returning
		relevantInputs.remove(propNet.getInitProposition());
		return relevantInputs;
	}

	private boolean setContainsAny(Set<Proposition> a, Set<Proposition> b){
		for (Object o: a){
			if (b.contains(o)) return true;
		}
		return false;
	}

	/**
	 * The list indexes by subgame, found by using findSubgameInputs on subterminal components.
	 * The mapping is from role to set of moves relevant to that subgame.
	 */
	private List<Map<Role, Set<Move>>> findRelevantMoves(){
		Set<Component> subterminalProps = new HashSet<Component>();
		Component terminalInput = propNet.getTerminalProposition().getSingleInput();

		// compute list of subterminal components
		Stack<Component> searchStack = new Stack<Component>();
		searchStack.push(terminalInput);
		while (!searchStack.isEmpty()){
			Component curr = searchStack.pop();
			if (curr instanceof Not || curr instanceof And){
				//System.out.println(curr);
				subterminalProps.add(curr);
			}
			else{
				for (Component c:curr.getInputs()){
					searchStack.push(c);
				}
			}
		}
		System.out.println("Potential Subterminal Components: "+subterminalProps.size());

		// finds inputs for each subterminal
		List<Set<Proposition>> subgameInputs = new ArrayList<Set<Proposition>>();
		for (Component start: subterminalProps){
			subgameInputs.add(findSubgameInputs(start));
		}

		// remove subgames with empty move sets
		for (int i=subgameInputs.size()-1; i>=0; i--){
			if (subgameInputs.get(i).size()==0){
				subgameInputs.remove(i);
			}
		}

		// removes subgames with overlapping inputs
		for (int i=subgameInputs.size()-2; i>=0; i--){
			for (int j=subgameInputs.size()-1; j>i; j--){
				if (setContainsAny(subgameInputs.get(i),subgameInputs.get(j))){
					subgameInputs.get(i).addAll(subgameInputs.get(j));
					subgameInputs.remove(j);
					//System.out.println("Removed subgame");
				}
			}
		}


		// initializes all subgame maps
		List<Map<Role, Set<Move>>> subgameInputMaps = new ArrayList<Map<Role, Set<Move>>>();
		for (int i=0; i<subgameInputs.size(); i++){
			subgameInputMaps.add(new HashMap<Role, Set<Move>>());
			for (Role role: getRoles()){
				Set<Move> initMoves = new HashSet<Move>();
				initMoves.add(null);
				subgameInputMaps.get(i).put(role, initMoves);
			}
		}


		// consider all legal moves for each role
		for (Role role: getRoles()){
			for (Proposition p: propNet.getLegalPropositions().get(role)){
				Proposition does = propNet.getLegalInputMap().get(p);

				// if a move is in any subgames, add it to corresponding map
				for (int i=0; i<subgameInputs.size(); i++){
					if (subgameInputs.get(i).contains(does)){
						Move m = new Move(p.getName().getBody().get(1));
						subgameInputMaps.get(i).get(role).add(m);
					}
				}
			}
		}

		System.out.println("Final Subterminal Components: "+subgameInputMaps.size());

		// print out statistics about subgames
		for (int i=0; i<subgameInputMaps.size(); i++){
			System.out.println("\nSUBGAME: " + i);
			for (Role role: getRoles()){
				System.out.println("\tROLE: "+role + " - NUM MOVES: " + subgameInputMaps.get(i).get(role).size());
			}
		}
		System.out.println();

		return subgameInputMaps;
	}



}

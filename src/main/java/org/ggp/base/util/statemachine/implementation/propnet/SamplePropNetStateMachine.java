package org.ggp.base.util.statemachine.implementation.propnet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;


@SuppressWarnings("unused")
public class SamplePropNetStateMachine extends StateMachine {
    /** The underlying proposition network  */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;

    private MachineState initialState;

    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override
    public void initialize(List<Gdl> description) {
    	print("PropNetStateMachine init");
        try {
            propNet = OptimizingPropNetFactory.create(description);
            roles = propNet.getRoles();
            ordering = getOrdering();
            print("Done init");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Computes if the state is terminal. Should return the value
     * of the terminal proposition for the state.
     */
    @Override
    public boolean isTerminal(MachineState state) {
        markBases(state);
//        return propmarkp(propNet.getTerminalProposition());
     // ** CS
        propagate();
        return propNet.getTerminalProposition().getValue();
    }

    /**
     * Computes the goal for a role in the current state.
     * Should return the value of the goal proposition that
     * is true for that role. If there is not exactly one goal
     * proposition true for that role, then you should throw a
     * GoalDefinitionException because the goal is ill-defined.
     */
    @Override
    public int getGoal(MachineState state, Role role)
            throws GoalDefinitionException {
    	// ** CS
    	markBases(state);
    	propagate();
    	int value = -1;
    	boolean foundOne = false;
		for (Proposition p: propNet.getGoalPropositions().get(role)) {
			if (p.getValue()) {
				// check if already found a goal. if so, throw exception
				if (foundOne) {
					throw new GoalDefinitionException(state, role);
				}

				value = getGoalValue(p);
				foundOne = true;
			}
		}

        return value;
    }

    /**
     * Returns the initial state. The initial state can be computed
     * by only setting the truth value of the INIT proposition to true,
     * and then computing the resulting state.
     */
    @Override
    public MachineState getInitialState() {
    	for (Proposition p: propNet.getBasePropositions().values()) {p.setValue(false);}
    	for (Proposition p: propNet.getInputPropositions().values()) {p.setValue(false);}
    	propNet.getInitProposition().setValue(true);
    	MachineState ret = getStateFromBase();
    	// unmark init proposition
    	propNet.getInitProposition().setValue(false);
    	return ret;
    }

    /**
     * Computes all possible actions for role.
     */
    @Override
    public List<Move> findActions(Role role)
            throws MoveDefinitionException {
		// we don't need to do this because we don't use it anywhere in our player
		return null;
    }

    /**
     * Computes the legal moves for role in state.
     */
    @Override
    public List<Move> getLegalMoves(MachineState state, Role role)
            throws MoveDefinitionException {
		List<Move> moves = new ArrayList<Move>();
		markBases(state);
		propagate();
		for (Proposition p: propNet.getLegalPropositions().get(role)) {
			if (p.getValue()) {
				moves.add(getMoveFromProposition(p));
			}
		}
        return moves;
    }

    /**
     * Computes the next state given state and the list of moves.
     */
    @Override
    public MachineState getNextState(MachineState state, List<Move> moves)
            throws TransitionDefinitionException {
    	// ** CS
    	markBases(state);
    	markInputs(moves);
    	propagate();
    	return getStateFromBase();
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
    public List<Proposition> getOrdering()
    {
        // List to contain the topological ordering.
        List<Proposition> order = new LinkedList<Proposition>();

        // All of the components in the PropNet
        List<Component> components = new ArrayList<Component>(propNet.getComponents());

        // All of the propositions in the PropNet.
        List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());

        // ** CS
        // first remove input and base propositions from this set of components
        // that we will be ordering topologically

        components.remove(propNet.getInitProposition());
        for (Proposition proposition : propNet.getInputPropositions().values()) {
			components.remove(proposition);
		}

		for (Proposition proposition : propNet.getBasePropositions().values()) {
			components.remove(proposition);
		}

		// order the rest of the components
		while (!components.isEmpty()) {
			// make list of components to be added next
			// based on whether their inputs have been added
			List<Component> toAdd = new ArrayList<Component>();
			for (Component component : components) {
				boolean shouldAdd = true;
				// check to make sure all inputs are already added
				for (Component input : component.getInputs()) {
					if (components.contains(input) && !toAdd.contains(input)) {
						shouldAdd = false;
						break; // found input that hasn't been added -- shouldn't add this component yet
					}
				}

				if (shouldAdd) {
					toAdd.add(component);
				}
			}

			// remove from components
			for (Component c: toAdd) {
				components.remove(c);
				if (c instanceof Proposition) {
					order.add((Proposition) c);
				}
			}
		}

        return order;
    }

    /* Already implemented for you */
    @Override
    public List<Role> getRoles() {
        return roles;
    }

    /* Helper methods */

    private void print(String s) {
    	System.out.println(s);
    }

    private boolean markBases(MachineState state)
    {
    	Map<GdlSentence, Proposition> baseProps = propNet.getBasePropositions();
    	Set<GdlSentence> stateContents = state.getContents();
    	for (GdlSentence key : baseProps.keySet()) {
    		if (stateContents.contains(key)) {
    			baseProps.get(key).setValue(true);
    		} else {
    			baseProps.get(key).setValue(false);
    		}
    	}
    	return true;
    }

    private void markInputs(List<Move> moves)
    {
    	Map<GdlSentence, Proposition> inputProps = propNet.getInputPropositions();
    	List<GdlSentence> actions = toDoes(moves);
    	for (GdlSentence key : inputProps.keySet()) {
    		if (actions.contains(key)) {
    			inputProps.get(key).setValue(true);
    		} else {
    			inputProps.get(key).setValue(false);
    		}
    	}
    }

    private void propagate() {
    	// ** CS
    	for (Proposition prop: ordering) {
			prop.setValue(prop.getSingleInput().getValue());
		}
    }



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
    public static Move getMoveFromProposition(Proposition p)
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
    public MachineState getStateFromBase()
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
}
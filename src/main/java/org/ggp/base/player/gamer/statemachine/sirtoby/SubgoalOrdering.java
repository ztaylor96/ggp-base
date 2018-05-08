package org.ggp.base.player.gamer.statemachine.sirtoby;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

import com.google.common.collect.ImmutableList;


public final class SubgoalOrdering extends StateMachineGamer
{

	private Map<MachineState, Integer> utils = new HashMap<MachineState, Integer>();
	private Map<MachineState, Integer> numVisits = new HashMap<MachineState, Integer>();

	@Override
	public String getName() {
		return "SubgoalOrdering";
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		long start = System.currentTimeMillis();
		long buffer = (long) ((timeout - start) * 0.05); // use 95% of available time
		long end = timeout - buffer;
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move move = moves.get(0);
		if (moves.size() > 1) {
			move = bestMove(getRole(), getCurrentState(), end);
		}
		notifyObservers(new GamerSelectedMoveEvent(moves, move, System.currentTimeMillis() - start));
		return move;
	}

	private Move bestMove(Role role, MachineState state, long end) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// explore successor states until time is up to estimate which has best expected utility by
		while (true) {
			// first heuristically select path to explore
			List<MachineState> path = new ArrayList<MachineState>();
			select(role, state, path);

			// explore starting from the last state of this path
			MachineState frontierState = path.get(path.size()-1);
			int value = explore(role, frontierState);

			// backprop this explored value upwards to other states in path
			for (MachineState s: path) {
				// skip updating frontier state because it already happened inside
				// explore function
				if (s.equals(frontierState)) { continue; }

				int stateCurrentUtil = 0;
				if (utils.containsKey(s)) { stateCurrentUtil = utils.get(s); }
				utils.put(s, stateCurrentUtil + value);

				int nVisits = 0;
				if (numVisits.containsKey(s)) { nVisits = numVisits.get(s); }
				numVisits.put(s, nVisits + 1);
			}

			if (System.currentTimeMillis() >= end) { break; }
		}

		// evaluate options, return move that leads to state with highest utility
		MachineState bestChild = null;
		int bestUtil = 0;
		Map<MachineState, Move> stateToMoveMap = new HashMap<MachineState, Move>();
		for (MachineState next: getSuccessorStates(role, state, stateToMoveMap)) {

			int util = 0;
			int nVisits = 1;
			if (getStateMachine().findTerminalp(next)) {
				util = getStateMachine().findReward(role, next);
				nVisits = 1;
			} else if (utils.containsKey(next)) {
				util = utils.get(next);
				nVisits = numVisits.get(next);
			}

			int averageUtil = util / nVisits;
			if (bestChild == null || averageUtil > bestUtil) {
				bestChild = next;
				bestUtil = averageUtil;
			}
		}

		return stateToMoveMap.get(bestChild);
	}

	private Set<MachineState> getSuccessorStates(Role r, MachineState s, Map<MachineState, Move> moves) throws MoveDefinitionException, TransitionDefinitionException {
		int playerIndex = getStateMachine().getRoleIndices().get(r);
		Set<MachineState> successors = new HashSet<MachineState>();
		for (List<Move> moveSeq: getStateMachine().getLegalJointMoves(s)) {
			MachineState successor = getStateMachine().findNext(moveSeq, s);
			successors.add(successor);
			if (moves != null) {
				moves.put(successor, moveSeq.get(playerIndex));
			}
		}
		return successors;
	}

	private void select(Role r, MachineState s, List<MachineState> path) throws MoveDefinitionException, TransitionDefinitionException {

		// if current node hasn't been visited, return it
		if (!numVisits.containsKey(s) || getStateMachine().findTerminalp(s)) {
			path.add(s);
			return;
		}

		// if any children haven't been visited, return that
		Set<MachineState> children = getSuccessorStates(r, s, null);
		double bestScore = 0.0;
		MachineState bestChild = null;
		for (MachineState child: children) {
			if (!numVisits.containsKey(child)) {
				path.add(child);
				return;
			}

			// compute scoring function for each child as we iterate through
			// because if all have been visited, we use the scoring function to choose
			// next child on path
			double score = computeScore(child, numVisits.get(s));
			if (bestChild == null || bestScore < score) {
				bestChild = child;
				bestScore = score;
			}
		}

		// add next child and continue building path
		path.add(bestChild);
		select(r, bestChild, path);
	}

	private double computeScore(MachineState s, int nParentVisits) {
		 int util = utils.get(s);
		 double nVisits = (double) numVisits.get(s);
		 return util / nVisits + Math.sqrt(2*Math.log(nParentVisits) / nVisits);
	}

	private int explore(Role r, MachineState s) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		int currentStateNumVisits = 0;
		if (numVisits.containsKey(s)) { currentStateNumVisits = numVisits.get(s); }
		numVisits.put(s, currentStateNumVisits + 1);

		if (getStateMachine().findTerminalp(s)) { // base condition: check if terminal state
			int reward = getStateMachine().findReward(r,s);
			utils.put(s, reward);
			return reward;
		}

		// pick a random move and recursively move down game tree
		List<List<Move>> allJointMoves = getStateMachine().getLegalJointMoves(s);
		List<Move> randomMoves = allJointMoves.get(ThreadLocalRandom.current().nextInt(0, allJointMoves.size()));
		MachineState next = getStateMachine().findNext(randomMoves, s);
		int value = explore(r, next);

		// update the total utility for this state based on exploration value
		int stateCurrentUtil = 0;
		if (utils.containsKey(s)) { stateCurrentUtil = utils.get(s); }
		utils.put(s, stateCurrentUtil + value);

		return value;	// propagate value upwards to previous states
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// Random gamer does no game previewing.
	}

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		ArrayList<Gdl> description = (ArrayList<Gdl>) getMatch().getGame().getRules();
		List<Gdl> newDesc = new ArrayList<Gdl>();

		for (Gdl gdl : description) {
            if (gdl instanceof GdlRule) {
                GdlRule rule = (GdlRule)gdl;
                newDesc.add(reorder(rule));
                //System.out.println("adding new rule");
            } else {
                //System.out.println("adding old gdl");
                newDesc.add(gdl);
            }
		}
		StateMachine newStateMachine = getInitialStateMachine();
		newStateMachine.initialize(newDesc);
		switchStateMachine(newStateMachine);
		System.out.println("Initialized new state machine");
	}

	private GdlRule reorder(GdlRule rule) {
		Set<GdlVariable> boundVars = new HashSet<GdlVariable>();
		//List<GdlSentence> subgoalsLeft = GdlUtils.getSentencesInRuleBody(rule);
		List<GdlLiteral> subgoalsLeft = new ArrayList<GdlLiteral>(rule.getBody());
		List<GdlLiteral> newOrdering = new ArrayList<GdlLiteral>();
		while (subgoalsLeft.size() > 0) {
			GdlLiteral ans = getBest(subgoalsLeft, boundVars);
		    newOrdering.add(ans);
		    for (GdlVariable v: GdlUtils.getVariablesSet(ans)) {
		    	boundVars.add(v);
		    }
		}
		return GdlPool.getRule(rule.getHead(), ImmutableList.copyOf(newOrdering));
	}

	private GdlLiteral getBest(List<GdlLiteral> subgoalsLeft, Set<GdlVariable> boundVars) {
		int varnum = 10000;
		int best = 0;
		for (int i = 0; i < subgoalsLeft.size(); i++) {
			int unboundNum = unboundvarnum(subgoalsLeft.get(i), boundVars);
			//System.out.println("unbound num:" + unboundNum);
	        if (unboundNum < varnum) {
	        	varnum = unboundNum;
	        	best = i;
	        }
	    }
		GdlLiteral ans = subgoalsLeft.get(best);
		subgoalsLeft.remove(best);
		return ans;

	}

	private int unboundvarnum(Gdl subgoal, Set<GdlVariable> boundVars) {
		return unboundvars(subgoal, new HashSet<GdlVariable>(), boundVars).size();
	}

	private Set<GdlVariable> unboundvars(Gdl subgoal, Set<GdlVariable> unboundVars, Set<GdlVariable> boundVars) {
		System.out.println(subgoal.getClass().getName());
		if (subgoal instanceof GdlProposition) {
			//System.out.println("Gdl Proposition");
			if (!boundVars.contains(subgoal)) {
				unboundVars.add((GdlVariable) subgoal);
			}
			return unboundVars;
		}
		if (subgoal instanceof GdlRelation) {
			//System.out.println("Gdl Relation");
			GdlRelation relation = (GdlRelation) subgoal;
			for (GdlTerm v: relation.getBody())
				if (v instanceof GdlVariable && !boundVars.contains(v)) {
					unboundVars.add((GdlVariable) v);
				}
			return unboundVars;
		}
		if (subgoal instanceof GdlLiteral) {
			//System.out.println("Gdl Literal");
			return unboundVars;
		}
		for (GdlVariable v: GdlUtils.getVariables(subgoal)) {
			unboundVars = unboundvars((Gdl) v,unboundVars, boundVars);
		}
		return unboundVars;
	}

	@Override
	public void stateMachineStop() {
		// Random gamer does no special cleanup when the match ends normally.
	}

	@Override
	public void stateMachineAbort() {
		// Random gamer does no special cleanup when the match ends abruptly.
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
	}
}
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
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.propnet.SamplePropNetStateMachine;


public final class PropNets extends StateMachineGamer
{

	private Map<MachineState, Integer> utils = new HashMap<MachineState, Integer>();
	private Map<MachineState, Integer> numVisits = new HashMap<MachineState, Integer>();

	@Override
	public String getName() {
		return "PropNets";
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		long start = System.currentTimeMillis();
		long end = timeout - 1500;
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		print("num legal from current state: " + moves.size());
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
		int nDepthCharges = 0;
		while (true) {
			// first heuristically select path to explore
			List<MachineState> path = new ArrayList<MachineState>();
			select(role, state, path);

			// explore starting from the last state of this path
			MachineState frontierState = path.get(path.size()-1);
			int value = explore(role, frontierState);
			nDepthCharges++;

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
		print("num depth charges: " + nDepthCharges);

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
		print("best util: "+bestUtil);

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
			print("found terminal reward value: " + reward);
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
//		return new CachedStateMachine(new ProverStateMachine());
		return new SamplePropNetStateMachine();
	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// Random gamer does no game previewing.
	}

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		print("metagaming");
	}

	private void print(String s) {
		System.out.println(s);
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
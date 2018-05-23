package org.ggp.base.player.gamer.statemachine.sirtoby;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;


public final class MonteCarlo extends StateMachineGamer
{

	private Map<MachineState, Integer> utils = new HashMap<MachineState, Integer>();
	private Map<MachineState, Integer> numVisits = new HashMap<MachineState, Integer>();

	@Override
	public String getName() {
		return "SirTobyMonteCarlo";
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{

		long start = System.currentTimeMillis();
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move move = moves.get(0);
		if (moves.size() > 1) {
			move = bestMove(getRole(), getCurrentState(), timeout - 1000); // 1 second buffer
		}
		notifyObservers(new GamerSelectedMoveEvent(moves, move, System.currentTimeMillis() - start));
		return move;
	}

	private Move bestMove(Role role, MachineState state, long end) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// explore successor states until time is up to estimate which has best expected utility by
		int sirTobyPlayerIndex = getStateMachine().getRoleIndices().get(role); // SirToby's index into moveSeq array below
		Map<MachineState, Move> moves = new HashMap<MachineState, Move>();	// map from successor State -> Move from current state
		while (true) {
			for (List<Move> moveSeq : getStateMachine().getLegalJointMoves(state)) {
				if (System.currentTimeMillis() >= end) { break; }
				MachineState next = getStateMachine().getNextState(state, moveSeq);
				explore(role, next);
				if (!moves.containsKey(next)) {
					moves.put(next, moveSeq.get(sirTobyPlayerIndex));	// remember which move leads us to next state
				}
			}

			if (System.currentTimeMillis() >= end) { break; }
		}

		// evaluate options, return move that leads to state with highest utility
		Move bestMove = null;
		int bestUtil = 0;
		for (MachineState key: moves.keySet()) {
			int nVisits = 1;	// for terminal states
			if (numVisits.containsKey(key)) {
				nVisits = numVisits.get(key);
			}

			int averageUtil = utils.get(key) / nVisits;
			if (bestMove == null || averageUtil > bestUtil) {
				bestMove = moves.get(key);
				bestUtil = averageUtil;
			}
		}

		return bestMove;
	}

	private int explore(Role r, MachineState s) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if (getStateMachine().findTerminalp(s)) { // base condition: check if terminal state
			int reward = getStateMachine().findReward(r,s);
			utils.put(s, reward);
			return reward;
		}

		// careful not to update this for terminal nodes because reward value
		// shouldn't be dampened by numVisits. For the code in bestMove to work,
		// we'll just use 1 as numVisits for terminal states when calculating best move
		int currentStateNumVisits = 0;
		if (numVisits.containsKey(s)) {
			currentStateNumVisits = numVisits.get(s);
		}
		numVisits.put(s, currentStateNumVisits + 1);

		// pick a random move and recursively move down game tree
		List<List<Move>> allJointMoves = getStateMachine().getLegalJointMoves(s);
		List<Move> randomMoves = allJointMoves.get(ThreadLocalRandom.current().nextInt(0, allJointMoves.size()));
		MachineState next = getStateMachine().findNext(randomMoves, s);
		int value = explore(r, next);

		// update the total utility for this state based on exploration value
		int stateCurrentUtil = 0;
		if (utils.containsKey(s)) {
			stateCurrentUtil = utils.get(s);
		}
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
		// Random gamer does no metagaming at the beginning of the match.
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
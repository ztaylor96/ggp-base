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

	@Override
	public String getName() {
		return "SirTobyMonteCarlo";
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{

		long start = System.currentTimeMillis();
		long buffer = (long) ((timeout - start) * 0.1); // use 95% of available time
		long end = timeout - buffer;
		Move move = bestMove(getRole(), getCurrentState(), end);
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		notifyObservers(new GamerSelectedMoveEvent(moves, move, System.currentTimeMillis() - start));
		return move;
	}

	private Move bestMove(Role role, MachineState state, long end) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// explore successor states until time is up to estimate which has best expected utility by
		Map<MachineState, Integer> utils = new HashMap<MachineState, Integer>();
		Map<MachineState, Move> moves = new HashMap<MachineState, Move>();
		int index = getStateMachine().getRoleIndices().get(role); // SirToby's index into moveSeq array below
		while (true) {
			for (List<Move> moveSeq : getStateMachine().getLegalJointMoves(state)) {
				if (System.currentTimeMillis() >= end) { break; }
				MachineState next = getStateMachine().getNextState(state, moveSeq);
				int util = explore(role, next);
				if (!utils.containsKey(next)) {
					utils.put(next, 0);
					moves.put(next, moveSeq.get(index));	// remember which move leads us to next state
				} else {
					utils.put(next, utils.get(next) + util); // update total utility for next state
				}
			}
			if (System.currentTimeMillis() >= end) { break; }
		}

		// evaluate options, return move that leads to state with highest utility
		Move bestMove = null;
		double bestUtil = 0.0;
		for (MachineState key: utils.keySet()) {
			if (bestMove == null || utils.get(key) > bestUtil) {
				bestMove = moves.get(key);
				bestUtil = utils.get(key);
			}
		}

		return bestMove;
	}

	private int explore(Role r, MachineState s) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		// check if terminal state
		if (getStateMachine().findTerminalp(s)) {
			return getStateMachine().findReward(r,s);
		}

		// pick a random move and recursively move down game tree
		List<List<Move>> allJointMoves = getStateMachine().getLegalJointMoves(s);
		List<Move> randomMoves = allJointMoves.get(ThreadLocalRandom.current().nextInt(0, allJointMoves.size()));
		MachineState next = getStateMachine().findNext(randomMoves, s);
		return explore(r, next);
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
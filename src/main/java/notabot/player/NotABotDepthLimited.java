package notabot.player;

import java.util.ArrayList;
import java.util.List;

import notabot.MovePath;
import notabot.NotABot;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class NotABotDepthLimited extends NotABot {

	private final int MAX_LEVEL = 15;

	@Override
	protected void runMetaGame() {
	}

	@Override
	protected Move getBestMove() throws MoveDefinitionException,
			TransitionDefinitionException, GoalDefinitionException {
		MovePath movePath = getBestMovePath(getCurrentState(), 0, true);
		return movePath.popMove();
	}

	/**
	 * Uses minimax to compute the best possible move assuming opponents are adversarial.
	 *
	 * @param state current state being analyzed
	 * @param isFirst only true for the first level of recursion
	 * @return the move path containing the best possible move
	 */
	private MovePath getBestMovePath(MachineState state, int level, boolean isFirst) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{

		// stop when reached max level
		if (level == MAX_LEVEL){
			System.out.println("HIT MAX LEVEL");
			return new MovePath(0);
		}

		// if state is terminal, return goal value of state
		if (getStateMachine().isTerminal(state) || hasTimedOut()){
			return new MovePath(getStateMachine().getGoal(state, getRole()));
		}

		// keeps track of best move, assuming opponents will do their best move
		MovePath bestWorst = null;

		// get moveset for each opponent; compute number of opponent combinations
		List<List<Move>> oppMoves = new ArrayList<List<Move>>();
		int numCombinations = 1;
		int ourRoleIndex = getStateMachine().getRoles().indexOf(getRole());
		for (Role role: getStateMachine().getRoles()){
			if (!role.equals(getRole())){
				List<Move> currMoves = getMoves(state, role);
				oppMoves.add(currMoves);
				numCombinations *= currMoves.size();
			}
		}

		// for each of our possible moves
		for (Move move: getMoves(state)){

			// keeps track of worst outcome with our current move
			MovePath worstBest = null;

			// compute every possible combination of opponent moves
			for (int i = 0; i < numCombinations; i++) {

				List<Move> moveCombo = new ArrayList<Move>();
				int divisor = 1;

				// get each opponent's move for current combination
				for (int opp = 0; opp < oppMoves.size(); opp++) {
					List<Move> currOppMoves = oppMoves.get(opp);
					moveCombo.add(currOppMoves.get(i/divisor % currOppMoves.size()));
					divisor *= currOppMoves.size();
				}

				moveCombo.add(ourRoleIndex, move);

				// Find the worst case with this combination
				MovePath currComboPath = getBestMovePath(getStateMachine().getNextState(state, moveCombo), level+1, false);


				// update worst outcome
				if (worstBest == null || worstBest.getEndStateGoal() > currComboPath.getEndStateGoal()){
					worstBest = currComboPath;
				}

			}

			// update best outcome of the worst outcomes
			if (bestWorst == null || bestWorst.getEndStateGoal() < worstBest.getEndStateGoal()){
				bestWorst = worstBest;
				bestWorst.pushMove(move);
			}

		}

		return bestWorst;
	}


}

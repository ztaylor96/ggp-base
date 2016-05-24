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

public class NotABotDepthLimitedHeuristic extends NotABot {


	private final int MAX_LEVEL = 2;
	private final double WEIGHT_ONE_STEP = 0;
	private final double WEIGHT_GOAL_PROX = 0;
	private final double WEIGHT_OPP_ONE_STEP = 0;
	private final double WEIGHT_MONTE_CARLO = 1;
	private final int MONTE_CARLO_SAMPLES = 10;




	private final double WEIGHT_TOTAL = WEIGHT_ONE_STEP+WEIGHT_GOAL_PROX+WEIGHT_OPP_ONE_STEP+WEIGHT_MONTE_CARLO;

	@Override
	protected void runMetaGame() {
		/*
		try{
			int maxSizeTree = 1;

			while (!hasTimedOut()){
				int sizeTree = 1;
				int depth = 0;

				MachineState curr = getCurrentState();
				while (!getStateMachine().isTerminal(curr)){
					curr = getStateMachine().getRandomNextState(curr);
					sizeTree *= getMoves(curr).size();
					depth ++;
				}

			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
		*/
	}

	@Override
	protected Move getBestMove() throws MoveDefinitionException,
			TransitionDefinitionException, GoalDefinitionException {

		if (getMoves().size()==1){
			return getMoves().get(0);
		}

		MovePath movePath = getBestMovePath(getCurrentState(), 0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true);
		return movePath.popMove();
	}

	private double heuristicOpponentMobility(MachineState state) throws MoveDefinitionException{

		int numOppMoves = 0;
		List<Role> roles = getStateMachine().getRoles();

		if (roles.size()==1) return 0;

		for (Role role: roles){
			if (!role.equals(getRole())){
				List<Move> currMoves = getMoves(state, role);
				numOppMoves += currMoves.size();
			}
		}
		return Math.min(numOppMoves/(roles.size()-1), 99);
	}

	private double heuristicOneStepMobility(MachineState state) throws MoveDefinitionException{
		// TODO can we figure out how to get the size of the action set from all possible moves
		return Math.min(getMoves(state).size(), 99);
	}

	private double heuristicGoalProximity(MachineState state) throws GoalDefinitionException{
		return getStateMachine().getGoal(state, getRole());
	}

	private double heuristicMonteCarlo(MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		long sumSamples = 0;
		for (int i=0; i<MONTE_CARLO_SAMPLES; i++){
			MachineState curr = state;
			while (!getStateMachine().isTerminal(curr))
				curr = getStateMachine().getRandomNextState(curr);
			sumSamples += getStateMachine().getGoal(curr, getRole());
		}
		return ((double)sumSamples) /  MONTE_CARLO_SAMPLES;
	}

	/**
	 * Computes a heuristic value of the current state
	 * @throws GoalDefinitionException
	 * @throws TransitionDefinitionException
	 */
	private int computeHeuristic(MachineState state) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException{
		Double heuristic = new Double(0);

		if (WEIGHT_ONE_STEP > 0) heuristic += heuristicOneStepMobility(state)*WEIGHT_ONE_STEP;
		if (WEIGHT_GOAL_PROX > 0) heuristic += heuristicOneStepMobility(state)*WEIGHT_GOAL_PROX;
		if (WEIGHT_OPP_ONE_STEP > 0) heuristic += heuristicOpponentMobility(state)*WEIGHT_OPP_ONE_STEP;
		if (WEIGHT_MONTE_CARLO > 0) heuristic += heuristicMonteCarlo(state)*WEIGHT_MONTE_CARLO;

		heuristic /= WEIGHT_TOTAL;

		System.out.println("HEURISTIC: " + heuristic);

		return heuristic.intValue();
	}

	/**
	 * Uses minimax to compute the best possible move assuming opponents are adversarial.
	 *
	 * @param state current state being analyzed
	 * @param isFirst only true for the first level of recursion
	 * @return the move path containing the best possible move
	 */
	private MovePath getBestMovePath(MachineState state, int level, double alpha, double beta, boolean isFirst) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		// if state is terminal, return goal value of state
		if (getStateMachine().isTerminal(state) || hasTimedOut()){
			return new MovePath(getStateMachine().getGoal(state, getRole()));
		}

		// stop when reached max level
		if (level == MAX_LEVEL){
			int heuristic = computeHeuristic(state);
			//System.out.println("HIT MAX LEVEL: "+heuristic);
			return new MovePath(heuristic);
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

			double minNodeBeta = beta;//

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
				MovePath currComboPath = getBestMovePath(getStateMachine().getNextState(state, moveCombo),
						level+1, alpha, beta, false);


				// update worst outcome
				if (worstBest == null || worstBest.getEndStateGoal() > currComboPath.getEndStateGoal()){
					worstBest = currComboPath;
				}

				// min node
				minNodeBeta = Math.min(minNodeBeta, currComboPath.getEndStateGoal());
				if (alpha >= minNodeBeta){
					//System.out.println("MIN NODE BREAK");
					break;
				}
			}

			// update best outcome of the worst outcomes
			if (bestWorst == null || bestWorst.getEndStateGoal() < worstBest.getEndStateGoal()){
				bestWorst = worstBest;
				bestWorst.pushMove(move);
			}

			if (isFirst){
				System.out.println(move + " : " + bestWorst.getEndStateGoal());
			}

			// max node
			alpha = Math.max(alpha, bestWorst.getEndStateGoal());
			if (alpha >= beta){
				//System.out.println("MAX NODE BREAK");
				break;
			}

		}

		return bestWorst;
	}


}

package notabot.gametree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import notabot.MoveScore;
import notabot.NotABot;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class GameTreeNode {

	protected final GameTree TREE;

	MachineState state;// the state this node represents
	int numMoveCombos;// the number of move combinations
	GameTreeNode[] children;// array of child nodes for all move combos
	int terminalGoal = 0;// contains player goal for terminal states
	boolean isTerminal;
	List<Move> playerMoves;
	int numPlayerMoves;
	List<List<Move>> allMoves;

	long numVisits = 0;
	long sumSamples = 0;

	int lastSelectMoveIndex;

	//boolean isExpanded = false;

	/**
	 * Constructor for any state except the initial state
	 */
	public GameTreeNode(MachineState state, GameTree gameTree){
		this(state, gameTree, null);
	}

	protected GameTreeNode(MachineState state, GameTree gameTree, Map<Role, Set<Move>> validMoves){
		TREE = gameTree;

		constructGameTreeNode(state, validMoves);
	}

	/**
	 * Constructor helper for GameTreeNode
	 */
	private void constructGameTreeNode(MachineState state, Map<Role, Set<Move>> validMoveMap){
		this.state = state;
		isTerminal = TREE.stateMachine.isTerminal(state);

		// If not terminal
		if (!isTerminal){
			// compute the number of move combinations possible
			numMoveCombos = 1;
			try{
				// no factoring
				if (validMoveMap == null){
					for (Role role: TREE.stateMachine.getRoles()){
						List<Move> currMoves = TREE.stateMachine.getLegalMoves(state, role);
						numMoveCombos *= currMoves.size();
					}
					children = new GameTreeNode[numMoveCombos];
					playerMoves = new ArrayList<Move>(TREE.stateMachine.getLegalMoves(state, TREE.roles.get(TREE.playerIndex)));
					numPlayerMoves = playerMoves.size();
				}
				// factoring
				else{
					Role playerRole = TREE.roles.get(TREE.playerIndex);
					allMoves = new ArrayList<List<Move>>();

					for (Role role: TREE.stateMachine.getRoles()){

						List<Move> currMoves = TREE.stateMachine.getLegalMoves(state, role);
						Set<Move> validMoves = validMoveMap.get(role);
						for (int i=currMoves.size()-1; i>=0; i--){
							Move move = currMoves.get(i);
							if (!validMoves.contains(move)){
								currMoves.remove(move);
							}
						}
						currMoves.add(null);

						allMoves.add(currMoves);

						if (role == playerRole){
							playerMoves = currMoves;
							numPlayerMoves = playerMoves.size();
						}

						numMoveCombos *= currMoves.size();
					}

					children = new GameTreeNode[numMoveCombos];

				}


				Collections.sort(playerMoves, TREE.moveComparator);
			}
			catch (MoveDefinitionException e){
				e.printStackTrace();
				numMoveCombos = 1;
			}

		}
		else{
			// compute goal for terminal nodes
			try {
				terminalGoal = TREE.stateMachine.getGoal(state, TREE.roles.get(TREE.playerIndex));
			}
			catch (GoalDefinitionException e) {
				e.printStackTrace();
				terminalGoal = 0;
			}
		}

	}

	/**
	 * Selection phase of MCTS
	 */
	GameTreeNode selectNode(List<GameTreeNode> path){
		path.add(this);

		// if node was just created
		if (isTerminal){
			return this;
		}

		// select first unexpanded child
		for (int i=0; i<numMoveCombos; i++){
			if (children[i] == null){
				createChild(i);
				lastSelectMoveIndex = i%numPlayerMoves;
				return children[i];
			}
		}

		// pick child with best heuristic score
		double bestScore = -1;
		GameTreeNode bestNode = this;
		int bestNodeIndex = 0;
		for (int i=0; i<numPlayerMoves; i++){
			int combo = i;

			if (playerMoves.get(i)==null && playerMoves.size()>1) continue;

			double worstScore = Double.POSITIVE_INFINITY;
			int worstCombo = -1;
			for (int j=0; j<numMoveCombos/numPlayerMoves; j++){
				GameTreeNode child = children[combo];
				double currScore = child.selectFunction(numVisits, numPlayerMoves, true);
				if (currScore < worstScore){
					worstCombo = combo;
					worstScore = currScore;
				}
				combo += numPlayerMoves;
			}

			worstScore = children[worstCombo].selectFunction(numVisits, numPlayerMoves, false);

			if (worstScore > bestScore){
				bestScore = worstScore;
				bestNode = children[worstCombo];
				bestNodeIndex = worstCombo;
			}
		}

		if (bestNode==null){
			System.out.println("SELECT PHASE RETURNED NULL");
		}

		lastSelectMoveIndex = bestNodeIndex%numPlayerMoves;

		return bestNode.selectNode(path);
	}

	/**
	 * Heuristic used during selection phase of MCTS
	 */
	double selectFunction(long parentNumVisits, int numMoves, boolean isOpponent){
		if (isOpponent){
			return getScore()/100 - Math.sqrt(Math.min(2,45./numMoves)*Math.log(parentNumVisits)/numVisits);
		}

		return getScore()/100 + Math.sqrt(45./numMoves*Math.log(parentNumVisits)/numVisits);
	}

	/**
	 * Expansion phase of MCTS
	 */
	void expandNode(int level){
		if (isTerminal){
			numVisits ++;
			return;
		}

		for (int i=0; i<numMoveCombos; i++){
			if (NotABot.hasTimedOut()) break;

			if (children[i]==null) createChild(i);

			if (level > 0){
				children[i].expandNode(level-1);
			}
			else{
				int goal = children[i].runSample();
				sumSamples += goal;
				numVisits ++;
			}
		}
	}

	/**
	 * TODO Unused
	 */
	public void buildTree(int expansionDepth){
		if (isTerminal) return;

		if (expansionDepth==0){
			runSample();
			return;
		}

		for (int i=0; i<numMoveCombos; i++){
			if (children[i] == null){
				createChild(i);
			}

			children[i].buildTree(expansionDepth-1);
		}
	}


	/**
	 * Run one sample from this node
	 * Simulation and backprop phases of MCTS
	 */
	public int runSample(){
		if (isTerminal){
			TREE.numDepthCharges++;
			return terminalGoal;
		}

		int combo = TREE.rand.nextInt(numMoveCombos);

		if (children[combo] == null){
			createChild(combo);
		}

		int goal = children[combo].runSample();
		sumSamples += goal;
		numVisits ++;

		return goal;
	}

	void visit(int goal){
		sumSamples += goal;
		numVisits ++;
		/*
		if (isTerminal && numVisits > 1000){
			System.out.println("ASDF : "+terminalGoal+" : " +numVisits + " : " + this);
		}
		else{
			System.out.println();
		}
		*/
	}

	/**
	 * Simulation phase of MCTS
	 */
	public int simulate(){
		TREE.numDepthCharges++;
		if (isTerminal){
			return terminalGoal;
		}

		MachineState curr = state;

		try {
			while (!TREE.stateMachine.isTerminal(curr)){
				curr = TREE.stateMachine.getRandomNextState(curr);
			}
			return TREE.stateMachine.getGoal(curr, TREE.roles.get(TREE.playerIndex));
		}
		catch (MoveDefinitionException | TransitionDefinitionException | GoalDefinitionException e) {
			e.printStackTrace();
		}

		return 0;
	}

	/**
	 * Returns the GameTreeNode child of this node whose state
	 * matches the given state
	 */
	public GameTreeNode getChildWithState(MachineState state){

		for (int i=0; i<numMoveCombos; i++){
			if (children[i] != null && children[i].isState(state)){
				//System.out.println("FOUND CHILD NODE IN TRAVERSE");
				return children[i];
			}

		}
		for (int i=0; i<numMoveCombos; i++){
			if (children[i] == null){
				createChild(i);
				if (children[i].isState(state)){
					//System.out.println("CREATED CHILD IN TRAVERSE");
					return children[i];
				}
			}
		}
		System.out.println("COULD NOT FIND CHILD");

		return null;
	}

	/**
	 * Computes the move set corresponding to the child index
	 * and creates the child, computing the corresponding state
	 */
	public void createChild(int combo){
		try {
			// compute move combo
			List<Move> moveCombo = new ArrayList<Move>();
			int divisor = numPlayerMoves;
			int playerMoveIndex = combo%numPlayerMoves;

			// get each opponent's move for current combination
			for (int r = 0; r < TREE.numRoles; r++) {
				if (r != TREE.playerIndex){
					List<Move> currMoves = TREE.stateMachine.getLegalMoves(state, TREE.roles.get(r));
					moveCombo.add(currMoves.get((combo/divisor) % currMoves.size()));
					divisor *= currMoves.size();
				}
				else{
					moveCombo.add(playerMoves.get(playerMoveIndex));
				}
			}

			// create child node
			children[combo] = new GameTreeNode(TREE.stateMachine.getNextState(state, moveCombo), TREE);
		}
		catch (MoveDefinitionException | TransitionDefinitionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns true if the given state matches the state of this node
	 */
	public boolean isState(MachineState state){
		return this.state.equals(state);
	}

	/**
	 *	Simple metric of average goal value during sampling
	 */
	public double getScore(){
		if (isTerminal) return terminalGoal;
		if (numVisits==0) return 0;
		return ((double) sumSamples)/numVisits;
	}

	/**
	 * Computes best move from this node using MiniMax with Alpha-Beta pruning
	 */
	public MoveScore getBestMove(int level, double alpha, double beta, boolean isFirst) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		// if state is terminal, return goal value of state
		if (isTerminal){
			return new MoveScore(terminalGoal);
		}

		// stop when reached max level
		if (level == 0){
			return new MoveScore(getScore());
		}


		// keeps track of best move, assuming opponents will do their best move
		MoveScore bestWorst = null;
		MoveScore nullScore = null;

		// for each of our possible moves
		//for (Move move: getMoves(state)){
		for (int i=0; i<numPlayerMoves; i++){

			// keeps track of worst outcome with our current move
			MoveScore worstBest = null;
			Move move = playerMoves.get(i);

			if (move==null && playerMoves.size()>1) continue;

			double minNodeBeta = beta;//
			int combo = i;

			// compute every possible combination of opponent moves
			//for (int i = 0; i < numCombinations; i++) {
			for (int j=0; j<numMoveCombos/numPlayerMoves; j++){

				// i is playerMoveIndex
				GameTreeNode child = children[combo];
				combo += numPlayerMoves;

				if (child!=null){
					// Find the worst case with this combination]
					MoveScore currMoveScore = child.getBestMove(level-1, alpha, minNodeBeta, false);
					currMoveScore.updateMove(move);

					// update worst outcome
					if (worstBest == null || worstBest.getScore() > currMoveScore.getScore()){
						worstBest = currMoveScore;
					}

					// min node
					minNodeBeta = Math.min(minNodeBeta, currMoveScore.getScore());
					if (alpha >= minNodeBeta){
						//System.out.println("MIN NODE BREAK");
						break;
					}
				}
				/*
				else{
					System.out.println("NULL CHILD DURING MINIMAX");
				}
				*/

			}

			if (isFirst){
				System.out.println("\t"+move + " : " + ((worstBest==null)?"?":worstBest.getScore()));
			}

			// update best outcome of the worst outcomes
			if (worstBest != null){
				if (playerMoves.get(i)!=null){
					if (bestWorst == null || bestWorst.getScore() < worstBest.getScore()){
						bestWorst = worstBest;
						bestWorst.updateMove(move);
					}

					// max node
					alpha = Math.max(alpha, bestWorst.getScore());
					if (alpha >= beta){
						//System.out.println("MAX NODE BREAK");
						break;
					}
				}
				else{
					nullScore = worstBest;
				}
			}
		}

		if (bestWorst == null){
			if (nullScore == null){
				System.out.println("MINIMAX FOUND NODE WITH ALL NULL CHILDREN");
				bestWorst = new MoveScore(0);
				bestWorst.updateMove(playerMoves.get(TREE.rand.nextInt(playerMoves.size())));
			}
			else{
				bestWorst = nullScore;
			}
		}

		return bestWorst;
	}



	/**
	 * Returns number of times this node was visited
	 */
	public long getNumVisits(){
		return numVisits;
	}

	/**
	 * Returns state corresponding to this node
	 */
	public MachineState getState(){
		return state;
	}

	/**
	 * Returns list of player moves from this node
	 */
	public List<Move> getPlayerMoves(){
		return playerMoves;
	}

	/**
	 * Returns the number of child nodes from this node
	 */
	public int getNumChildren(){
		return children.length;
	}

	/**
	 * Returns the child corresponding to the given combo index
	 */
	public GameTreeNode getChild(int index){
		if (index<0 || index >= children.length) return null;
		return children[index];
	}

	/**
	 * Used for NotABotTreeVisualizer
	 * Returns move index corresponding to last selected child
	 */
	public int getLastSelectMoveIndex(){
		return lastSelectMoveIndex;
	}

	/*
	boolean updateIsExpanded(){
		if (isExpanded) return true;

		if (isTerminal){
			isExpanded = true;
			return true;
		}

		for (int i=0; i<numMoveCombos; i++){
			if (children[i] == null || !children[i].isExpanded){
				return false;
			}
		}

		isExpanded = true;
		return true;
	}
	*/
}

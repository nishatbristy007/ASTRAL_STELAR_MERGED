package phylonet.coalescent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import phylonet.network.io.TripartitionsReader;
import phylonet.tree.model.TMutableNode;
import phylonet.tree.model.TNode;
import phylonet.tree.model.Tree;
import phylonet.tree.model.sti.STINode;
import phylonet.tree.model.sti.STITree;
import phylonet.tree.model.sti.STITreeCluster;
import phylonet.tree.model.sti.STITreeClusterWD;
import phylonet.util.BitSet;

/**
 * Knows how to compute the score of a given tripartition
 * 
 * @author smirarab
 * 
 */
class WQWeightCalculator extends AbstractWeightCalculator<Tripartition> {

	WQInference inference;
	private WQDataCollection dataCollection;
	WeightCalculatorAlgorithm algorithm;
	private WeightCalculatorAlgorithm tmpalgorithm;
	private Map<STBipartition, Integer> geneTreeSTBCount;


	public WQWeightCalculator(AbstractInference<Tripartition> inference) {
		super(false);
		this.dataCollection = (WQDataCollection) inference.dataCollection;
		this.inference = (WQInference) inference;
		//this.algorithm = new TraversalWeightCalculator();
		//this.algorithm = new SetWeightCalculator();
		//this.tmpalgorithm = new SetWeightCalculator();
		this.algorithm = new CondensedTraversalWeightCalculator();
		tmpalgorithm = new TraversalWeightCalculator();
		//tmpalgorithm.setupGeneTrees((WQInference) inference);
	}

	abstract class WeightCalculatorAlgorithm {
		long finalWeight = 0;
		long F(long a, long b, long c) {
			if (a < 0 || b < 0 || c < 0) {
				throw new RuntimeException("negative side not expected: " + a
						+ " " + b + " " + c);
			}
			long ret = (a + b + c - 3);
			ret *= a * b * c;
			return ret;
		}
		long F2(long a, long b) {
			if (a < 0 || b < 0) {
				throw new RuntimeException("negative side not expected: " + a
						+ " " + b );
			}
			long ret = (a + b - 2);
			ret *= a * b/2.0;
			return ret;
		}

		abstract Long calculateWeight(Tripartition trip);

		abstract void setupGeneTrees(WQInference inference);

		abstract Long calculateWeightTriplet(STBipartition trip);
	}

	@Override
	Long calculateWeight(Tripartition t,
			AbstractComputeMinCostTask<Tripartition> minCostTask) {
		return this.algorithm.calculateWeight(t);
	}

	@Override
	Long calculateWeightTriplet(STBipartition t,AbstractComputeMinCostTask<Tripartition> minCostTask) {
		return this.algorithm.calculateWeightTriplet(t);
	}

	/**
	 * one of ASTRAL-III way of calculating weights
	 * Should be memory efficient
	 * @author chaoszhang
	 *
	 */
	class CondensedTraversalWeightCalculator extends WeightCalculatorAlgorithm {
		Polytree polytree;
		Tripartition[] finalTripartitions = null;
		STBipartition[] finalSTBipartitions = null;
		int[] finalSTBcounts = null;
		
		Long calculateWeight(Tripartition trip) {
			return polytree.WQWeightByTraversal(trip, this);
		}
		Long calculateWeightTriplet(STBipartition trip) {
			long tpweight = 0l;
			for(int i = 0;i<this.finalSTBipartitions.length;i++)
			{
				tpweight += sharedTripletCount(trip, this.finalSTBipartitions[i]) * this.finalSTBcounts[i];
			}
			return tpweight;
		}

		/***
		* Each gene tree is represented as a list of integers, using positive numbers
		* for leaves, where the number gives the index of the leaf. 
		* We use negative numbers for internal nodes, where the value gives the number of children. 
		* Minus infinity is used for separating different genes. 
		*/
		@Override
		void setupGeneTrees(WQInference inference) {
			System.err.println("Using polytree-based weight calculation.");
			polytree = new Polytree(inference.trees, dataCollection);
			setupGeneTreesForSTB(inference);
		}

		private void addSTBipartition(STITreeCluster l_cluster,STITreeCluster r_cluster,TNode node,
			Map<STBipartition, Integer> geneTreeSTBCount) {

			STBipartition bi = new STBipartition(l_cluster, r_cluster);
			geneTreeSTBCount.put(bi, geneTreeSTBCount.containsKey(bi) ? geneTreeSTBCount.get(bi) + 1 : 1);
		}

		//private Map<AbstractMap.SimpleEntry<STITreeCluster, STITreeCluster>, Integer> geneTreeInvalidSTBCont;
		void setupGeneTreesForSTB(WQInference inference)
		{
			List<STITreeCluster> treeCompteleClusters = ((WQDataCollection) inference.dataCollection).treeAllClusters;
			List<Tree> geneTrees = inference.treesRooted;

			System.err.println("Calculating tripartitions from gene trees ");

			geneTreeSTBCount = new HashMap<STBipartition, Integer>(inference.trees.size()
								* GlobalMaps.taxonIdentifier.taxonCount());

			int t = 0;
			for (Tree tr : geneTrees) {
				 
				Stack<STITreeCluster> stack = new Stack<STITreeCluster>();
				STITreeCluster gtAll = treeCompteleClusters.get(t++);
				BitSet gtAllBS = gtAll.getBitSet();

				for (TNode node : tr.postTraverse()) {
					if (node.isLeaf()) {
						STITreeCluster cluster = GlobalMaps.taxonIdentifier
								.getClusterForNodeName(node.getName());
						stack.add(cluster);
					} else {

						ArrayList<STITreeCluster> childbslist = new ArrayList<STITreeCluster>();
						BitSet bs = new BitSet(
								GlobalMaps.taxonIdentifier.taxonCount());
						for (TNode child : node.getChildren()) {
							STITreeCluster pop = stack.pop();
							childbslist.add(pop);
							bs.or(pop.getBitSet());
						}

						STITreeCluster cluster = GlobalMaps.taxonIdentifier
								.newCluster();
						;
						cluster.setCluster((BitSet) bs.clone());
						stack.add(cluster);

						for (int i = 0; i < childbslist.size(); i++) {
							for (int j = i + 1; j < childbslist.size(); j++) {
									addSTBipartition(childbslist.get(0),childbslist.get(1), node,geneTreeSTBCount);
							}
						}
						

					}
				}

			}

			System.err.println("Using Bipartition-based weight calculation.");

			finalSTBipartitions = new STBipartition[geneTreeSTBCount.size()];
			finalSTBcounts = new int[geneTreeSTBCount.size()];
			int i = 0;
			for (Entry<STBipartition, Integer> entry : geneTreeSTBCount.entrySet()) {
				finalSTBipartitions[i] = entry.getKey();
				finalSTBcounts[i] = entry.getValue();
				i++;
			}

			if (geneTreeSTBCount.size() > 0) {
				long s = 0;
				for (Integer c : geneTreeSTBCount.values()) {
					s += c;
				}
				System.err.println("Bipartitions in gene trees (count): "
						+ geneTreeSTBCount.size());
				System.err.println("Bipartitions in gene trees (sum): " + s);
			}
		}

		long sharedTripletCount(STBipartition that, STBipartition other)
		{
			int I0 = that.cluster1.getBitSet().intersectionSize(other.cluster1.getBitSet());
			int I1 = that.cluster1.getBitSet().intersectionSize(other.cluster2.getBitSet());
			int I2 = that.cluster2.getBitSet().intersectionSize(other.cluster1.getBitSet());
			int I3 = that.cluster2.getBitSet().intersectionSize(other.cluster2.getBitSet());
			
			long sum = F2(I0,I3) + F2(I1,I2);
			return sum;
		}

	}
	
	
	/**
	 * ASTRAL-II way of calculating weights 
	 * @author smirarab
	 * 
	 */
	class TraversalWeightCalculator extends WeightCalculatorAlgorithm {

		int[][] stack = new int[GlobalMaps.taxonIdentifier.taxonCount() + 2][3];

		int[][] overlap = new int[GlobalMaps.taxonIdentifier.taxonCount() + 1][3];
		int[][] overlapind = new int[GlobalMaps.taxonIdentifier.taxonCount() + 1][3];

		Integer[] geneTreesAsInts;

		Tripartition[] finalTripartitions = null;
		STBipartition[] finalSTBipartitions = null;
		int[] finalSTBcounts = null;

		Long calculateWeight(Tripartition trip) {

			long weight = 0;
			int[] allsides = null;
			Iterator<STITreeCluster> tit = dataCollection.treeAllClusters
					.iterator();
			boolean newTree = true;
			int top = 0; // The first empty place on stack (generally)
			for (Integer gtb : this.geneTreesAsInts) {
				if (newTree) {
					STITreeCluster all = tit.next();
					allsides = new int[] {
							trip.cluster1.getBitSet().intersectionSize(
									all.getBitSet()),
							trip.cluster2.getBitSet().intersectionSize(
									all.getBitSet()),
							trip.cluster3.getBitSet().intersectionSize(
									all.getBitSet()) };
					newTree = false;
				}
				if (gtb >= 0) { // Leaf nodes
					if (trip.cluster1.getBitSet().get(gtb)) {
						stack[top][0] = 1;
						stack[top][1] = 0;
						stack[top][2] = 0;
					} else if (trip.cluster2.getBitSet().get(gtb)) {
						stack[top][0] = 0;
						stack[top][1] = 1;
						stack[top][2] = 0;
					} else if (trip.cluster3.getBitSet().get(gtb)) {
						stack[top][0] = 0;
						stack[top][1] = 0;
						stack[top][2] = 1;
					} else { // This can happen due to missing data
						stack[top][0] = 0;
						stack[top][1] = 0;
						stack[top][2] = 0;
					}
					top++;
				} else if (gtb == Integer.MIN_VALUE) { // delimiter between
														// trees
					top = 0;
					newTree = true;
				} else if (gtb == -2) { // Internal nodes

					top--;
					int newSides0 = stack[top][0] + stack[top - 1][0];
					int newSides1 = stack[top][1] + stack[top - 1][1];
					int newSides2 = stack[top][2] + stack[top - 1][2];

					int side3s0 = allsides[0] - newSides0;
					int side3s1 = allsides[1] - newSides1;
					int side3s2 = allsides[2] - newSides2;

					weight += F(stack[top][0], stack[top - 1][1], side3s2)
							+ F(stack[top][0], stack[top - 1][2], side3s1)
							+ F(stack[top][1], stack[top - 1][0], side3s2)
							+ F(stack[top][1], stack[top - 1][2], side3s0)
							+ F(stack[top][2], stack[top - 1][0], side3s1)
							+ F(stack[top][2], stack[top - 1][1], side3s0);

					stack[top - 1][0] = newSides0;
					stack[top - 1][1] = newSides1;
					stack[top - 1][2] = newSides2;
				} else { // The following case is relevant only for polytomies.

					int[] nzc = { 0, 0, 0 };
					int[] newSides = { 0, 0, 0 };
					for (int side = 0; side < 3; side++) {
						for (int i = top - 1; i >= top + gtb; i--) {
							if (stack[i][side] > 0) {
								newSides[side] += stack[i][side];
								overlap[nzc[side]][side] = stack[i][side];
								overlapind[nzc[side]++][side] = i;
							}
						}
						stack[top][side] = allsides[side] - newSides[side];

						if (stack[top][side] > 0) {
							overlap[nzc[side]][side] = stack[top][side];
							overlapind[nzc[side]++][side] = top;
						}
						stack[top + gtb][side] = newSides[side];
					}

					for (int i = nzc[0] - 1; i >= 0; i--) {
						for (int j = nzc[1] - 1; j >= 0; j--) {
							if (overlapind[i][0] != overlapind[j][1])
								for (int k = nzc[2] - 1; k >= 0; k--) {
									if ((overlapind[i][0] != overlapind[k][2])
											&& (overlapind[j][1] != overlapind[k][2]))
										weight += F(overlap[i][0],
												overlap[j][1],
												overlap[k][2]);
								}
						}
					}
				
					top = top + gtb + 1;

				} // End of polytomy section

			}

			return weight;
		}
		Long calculateWeightTriplet(STBipartition trip) {
			long tpweight = 0l;
			for(int i = 0;i<this.finalSTBipartitions.length;i++)
			{
				tpweight += sharedTripletCount(trip, this.finalSTBipartitions[i]) * this.finalSTBcounts[i];
			}
			return tpweight;
		}


		/***
		* Each gene tree is represented as a list of integers, using positive numbers
		* for leaves, where the number gives the index of the leaf. 
		* We use negative numbers for internal nodes, where the value gives the number of children. 
		* Minus infinity is used for separating different genes. 
		*/
		@Override
		void setupGeneTrees(WQInference inference) {
			System.err.println("Using tree-based weight calculation.");
			List<Integer> temp = new ArrayList<Integer>();

			for (Tree tr : inference.trees) {
				List<STINode> children = new ArrayList<STINode>();
				int n = tr.getLeafCount()/2;
				int dist = n;
				TNode newroot = tr.getRoot();
				for (TNode node : tr.postTraverse()) {
					if (!node.isLeaf()) {                        
						for (TNode child : node.getChildren()) {
							if (child.isLeaf()) {
								children.add((STINode) child);
								break;
							}
						}
						if (Math.abs(n - node.getLeafCount()) < dist) {
							newroot = node;
							dist = n - node.getLeafCount();
						}
					}
				}
				// Make the tree left-heavy so that the stack gets small
				for (STINode child: children) {
						STINode snode = child.getParent();
						snode.removeChild((TMutableNode) child, false);
						TMutableNode newChild = snode.createChild(child);
						if (child == newroot) {
							newroot = newChild;
						}
				}
				if (newroot != tr.getRoot()){
					((STITree)(tr)).rerootTreeAtEdge(newroot);
				}
				for (TNode node : tr.postTraverse()) {
					if (node.isLeaf()) {
						temp.add(GlobalMaps.taxonIdentifier.taxonId(node.getName()));
					} else {
						temp.add(-node.getChildCount());
					}
					if (node.isRoot()) {
						temp.add(Integer.MIN_VALUE);
					}
				}
			}
			geneTreesAsInts = temp.toArray(new Integer[] {});
			setupGeneTreesForSTB(inference);
		}
		
		private void addSTBipartition(STITreeCluster l_cluster,STITreeCluster r_cluster,TNode node,
			Map<STBipartition, Integer> geneTreeSTBCount) {

			STBipartition bi = new STBipartition(l_cluster, r_cluster);
			geneTreeSTBCount.put(bi, geneTreeSTBCount.containsKey(bi) ? geneTreeSTBCount.get(bi) + 1 : 1);
		}

		void setupGeneTreesForSTB(WQInference inference)
		{
			List<STITreeCluster> treeCompteleClusters = ((WQDataCollection) inference.dataCollection).treeAllClusters;
			List<Tree> geneTrees = inference.treesRooted;

			System.err.println("Calculating tripartitions from gene trees ");

			geneTreeSTBCount = new HashMap<STBipartition, Integer>(inference.trees.size()
								* GlobalMaps.taxonIdentifier.taxonCount());

			int t = 0;
			for (Tree tr : geneTrees) {
				 
				Stack<STITreeCluster> stack = new Stack<STITreeCluster>();
				STITreeCluster gtAll = treeCompteleClusters.get(t++);
				BitSet gtAllBS = gtAll.getBitSet();

				for (TNode node : tr.postTraverse()) {
					if (node.isLeaf()) {
						STITreeCluster cluster = GlobalMaps.taxonIdentifier
								.getClusterForNodeName(node.getName());
						stack.add(cluster);
					} else {

						ArrayList<STITreeCluster> childbslist = new ArrayList<STITreeCluster>();
						BitSet bs = new BitSet(
								GlobalMaps.taxonIdentifier.taxonCount());
						for (TNode child : node.getChildren()) {
							STITreeCluster pop = stack.pop();
							childbslist.add(pop);
							bs.or(pop.getBitSet());
						}

						STITreeCluster cluster = GlobalMaps.taxonIdentifier
								.newCluster();
						;
						cluster.setCluster((BitSet) bs.clone());
						stack.add(cluster);

						for (int i = 0; i < childbslist.size(); i++) {
							for (int j = i + 1; j < childbslist.size(); j++) {
									addSTBipartition(childbslist.get(0),childbslist.get(1), node,geneTreeSTBCount);
							}
						}
						

					}
				}

			}

			System.err.println("Using Bipartition-based weight calculation.");

			finalSTBipartitions = new STBipartition[geneTreeSTBCount.size()];
			finalSTBcounts = new int[geneTreeSTBCount.size()];
			int i = 0;
			for (Entry<STBipartition, Integer> entry : geneTreeSTBCount.entrySet()) {
				finalSTBipartitions[i] = entry.getKey();
				finalSTBcounts[i] = entry.getValue();
				i++;
			}

			if (geneTreeSTBCount.size() > 0) {
				long s = 0;
				for (Integer c : geneTreeSTBCount.values()) {
					s += c;
				}
				System.err.println("Bipartitions in gene trees (count): "
						+ geneTreeSTBCount.size());
				System.err.println("Bipartitions in gene trees (sum): " + s);
			}
		}

		long sharedTripletCount(STBipartition that, STBipartition other)
		{
			int I0 = that.cluster1.getBitSet().intersectionSize(other.cluster1.getBitSet());
			int I1 = that.cluster1.getBitSet().intersectionSize(other.cluster2.getBitSet());
			int I2 = that.cluster2.getBitSet().intersectionSize(other.cluster1.getBitSet());
			int I3 = that.cluster2.getBitSet().intersectionSize(other.cluster2.getBitSet());
			
			long sum = F2(I0,I3) + F2(I1,I2);
			return sum;
		}


	}

	/***
	 * This is for ASTRAL-I
	 * 
	 * @author smirarab
	 * 
	 */
	class SetWeightCalculator extends WeightCalculatorAlgorithm {

		Tripartition[] finalTripartitions = null;
		STBipartition[] finalSTBipartitions = null;
		int[] finalSTBcounts = null;
		int[] finalCounts = null;
		Integer[] geneTreesAsInts;


		Long calculateWeight(Tripartition trip) {
			long qtweight = 0l,tpweight = 0l;
			for (int i = 0; i < this.finalCounts.length; i++) {
				qtweight += sharedQuartetCount(trip, this.finalTripartitions[i])
						* this.finalCounts[i];
			}
			finalWeight =  qtweight;
			return finalWeight;
		}
		Long calculateWeightTriplet(STBipartition trip)
		{
			long tpweight = 0l;
			for(int i = 0;i<this.finalSTBipartitions.length;i++)
			{
				tpweight += sharedTripletCount(trip, this.finalSTBipartitions[i]) * this.finalSTBcounts[i];
			}
			return tpweight;
		}

		private void addTripartition(STITreeCluster l_cluster,
				STITreeCluster r_cluster, STITreeCluster remaining, TNode node,
				Map<Tripartition, Integer> geneTreeTripartitonCount) {

			Tripartition trip = new Tripartition(l_cluster, r_cluster,
					remaining);
			geneTreeTripartitonCount.put(trip, geneTreeTripartitonCount
					.containsKey(trip) ? geneTreeTripartitonCount.get(trip) + 1
					: 1);
		}

		void setupGeneTreesForGeneTreesInt(WQInference inference) {
			System.err.println("Using tree-based weight calculation.");
			List<Integer> temp = new ArrayList<Integer>();

			for (Tree tr : inference.trees) {
				List<STINode> children = new ArrayList<STINode>();
				int n = tr.getLeafCount()/2;
				int dist = n;
				TNode newroot = tr.getRoot();
				for (TNode node : tr.postTraverse()) {
					if (!node.isLeaf()) {                        
						for (TNode child : node.getChildren()) {
							if (child.isLeaf()) {
								children.add((STINode) child);
								break;
							}
						}
						if (Math.abs(n - node.getLeafCount()) < dist) {
							newroot = node;
							dist = n - node.getLeafCount();
						}
					}
				}
				// Make the tree left-heavy so that the stack gets small
				for (STINode child: children) {
						STINode snode = child.getParent();
						snode.removeChild((TMutableNode) child, false);
						TMutableNode newChild = snode.createChild(child);
						if (child == newroot) {
							newroot = newChild;
						}
				}
				if (newroot != tr.getRoot()){
					((STITree)(tr)).rerootTreeAtEdge(newroot);
				}
				for (TNode node : tr.postTraverse()) {
					if (node.isLeaf()) {
						temp.add(GlobalMaps.taxonIdentifier.taxonId(node.getName()));
					} else {
						temp.add(-node.getChildCount());
					}
					if (node.isRoot()) {
						temp.add(Integer.MIN_VALUE);
					}
				}
			}
			geneTreesAsInts = temp.toArray(new Integer[] {});

		}

		

		void setupGeneTrees(WQInference inference) {
			setupGeneTreesForGeneTreesInt(inference);
			List<STITreeCluster> treeCompteleClusters = ((WQDataCollection) inference.dataCollection).treeAllClusters;
			List<Tree> geneTrees = inference.trees;

			System.err.println("Calculating tripartitions from gene trees ");

			Map<Tripartition, Integer> geneTreeTripartitonCount = new HashMap<Tripartition, Integer>(
					inference.trees.size()
							* GlobalMaps.taxonIdentifier.taxonCount());

			int t = 0;
			for (Tree tr : geneTrees) {
				// System.err.print(".");
				Stack<STITreeCluster> stack = new Stack<STITreeCluster>();
				STITreeCluster gtAll = treeCompteleClusters.get(t++);
				BitSet gtAllBS = gtAll.getBitSet();

				for (TNode node : tr.postTraverse()) {
					if (node.isLeaf()) {
						STITreeCluster cluster = GlobalMaps.taxonIdentifier
								.getClusterForNodeName(node.getName());
						stack.add(cluster);
					} else {

						ArrayList<STITreeCluster> childbslist = new ArrayList<STITreeCluster>();
						BitSet bs = new BitSet(
								GlobalMaps.taxonIdentifier.taxonCount());
						for (TNode child : node.getChildren()) {
							STITreeCluster pop = stack.pop();
							childbslist.add(pop);
							bs.or(pop.getBitSet());
						}

						STITreeCluster cluster = GlobalMaps.taxonIdentifier
								.newCluster();
						;
						cluster.setCluster((BitSet) bs.clone());
						stack.add(cluster);

						STITreeCluster remaining = cluster
								.complementaryCluster();
						remaining.getBitSet().and(gtAllBS);
						if (remaining.getClusterSize() != 0) {
							childbslist.add(remaining);
						}
						// System.err.println(childbslist.size());
						for (int i = 0; i < childbslist.size(); i++) {
							for (int j = i + 1; j < childbslist.size(); j++) {
								for (int k = j + 1; k < childbslist.size(); k++) {

									addTripartition(childbslist.get(i),
											childbslist.get(j),
											childbslist.get(k), node,
											geneTreeTripartitonCount);
								}
							}
						}

					}
				}

			}

			System.err.println("Using tripartition-based weight calculation.");

			finalTripartitions = new Tripartition[geneTreeTripartitonCount
					.size()];
			finalCounts = new int[geneTreeTripartitonCount.size()];
			int i = 0;
			for (Entry<Tripartition, Integer> entry : geneTreeTripartitonCount
					.entrySet()) {
				finalTripartitions[i] = entry.getKey();
				finalCounts[i] = entry.getValue();
				//System.out.println("final trip "+ finalTripartitions[i].toString()+" "+finalCounts[i]);
				i++;
			}

			if (geneTreeTripartitonCount.size() > 0) {
				long s = 0;
				for (Integer c : geneTreeTripartitonCount.values()) {
					s += c;
				}
				System.err.println("Tripartitions in gene trees (count): "
						+ geneTreeTripartitonCount.size());
				System.err.println("Tripartitions in gene trees (sum): " + s);
			}

			setupGeneTreesForSTB(inference);
		}

		private void addSTBipartition(STITreeCluster l_cluster,STITreeCluster r_cluster,TNode node,
			Map<STBipartition, Integer> geneTreeSTBCount) {

			STBipartition bi = new STBipartition(l_cluster, r_cluster);
			geneTreeSTBCount.put(bi, geneTreeSTBCount.containsKey(bi) ? geneTreeSTBCount.get(bi) + 1 : 1);
		}

		//private Map<AbstractMap.SimpleEntry<STITreeCluster, STITreeCluster>, Integer> geneTreeInvalidSTBCont;
		void setupGeneTreesForSTB(WQInference inference)
		{
			List<STITreeCluster> treeCompteleClusters = ((WQDataCollection) inference.dataCollection).treeAllClusters;
			List<Tree> geneTrees = inference.treesRooted;

			System.err.println("Calculating tripartitions from gene trees ");

			geneTreeSTBCount = new HashMap<STBipartition, Integer>(inference.trees.size()
								* GlobalMaps.taxonIdentifier.taxonCount());

			int t = 0;
			for (Tree tr : geneTrees) {
				 
				Stack<STITreeCluster> stack = new Stack<STITreeCluster>();
				STITreeCluster gtAll = treeCompteleClusters.get(t++);
				BitSet gtAllBS = gtAll.getBitSet();

				for (TNode node : tr.postTraverse()) {
					if (node.isLeaf()) {
						STITreeCluster cluster = GlobalMaps.taxonIdentifier
								.getClusterForNodeName(node.getName());
						stack.add(cluster);
					} else {

						ArrayList<STITreeCluster> childbslist = new ArrayList<STITreeCluster>();
						BitSet bs = new BitSet(
								GlobalMaps.taxonIdentifier.taxonCount());
						for (TNode child : node.getChildren()) {
							STITreeCluster pop = stack.pop();
							childbslist.add(pop);
							bs.or(pop.getBitSet());
						}

						STITreeCluster cluster = GlobalMaps.taxonIdentifier
								.newCluster();
						;
						cluster.setCluster((BitSet) bs.clone());
						stack.add(cluster);

						for (int i = 0; i < childbslist.size(); i++) {
							for (int j = i + 1; j < childbslist.size(); j++) {
									addSTBipartition(childbslist.get(0),childbslist.get(1), node,geneTreeSTBCount);
							}
						}
						

					}
				}

			}

			System.err.println("Using Bipartition-based weight calculation.");

			finalSTBipartitions = new STBipartition[geneTreeSTBCount.size()];
			finalSTBcounts = new int[geneTreeSTBCount.size()];
			int i = 0;
			for (Entry<STBipartition, Integer> entry : geneTreeSTBCount.entrySet()) {
				finalSTBipartitions[i] = entry.getKey();
				finalSTBcounts[i] = entry.getValue();
				i++;
			}

			if (geneTreeSTBCount.size() > 0) {
				long s = 0;
				for (Integer c : geneTreeSTBCount.values()) {
					s += c;
				}
				System.err.println("Bipartitions in gene trees (count): "
						+ geneTreeSTBCount.size());
				System.err.println("Bipartitions in gene trees (sum): " + s);
			}
		}

		long sharedTripletCount(STBipartition that, STBipartition other)
		{
			int I0 = that.cluster1.getBitSet().intersectionSize(other.cluster1.getBitSet());
			int I1 = that.cluster1.getBitSet().intersectionSize(other.cluster2.getBitSet());
			int I2 = that.cluster2.getBitSet().intersectionSize(other.cluster1.getBitSet());
			int I3 = that.cluster2.getBitSet().intersectionSize(other.cluster2.getBitSet());
			
			long sum = F2(I0,I3) + F2(I1,I2);
			return sum;
		}

	
		long sharedQuartetCount(Tripartition that, Tripartition other) {
			int I0 = that.cluster1.getBitSet().intersectionSize(
					other.cluster1.getBitSet()), I1 = that.cluster1.getBitSet()
					.intersectionSize(other.cluster2.getBitSet()), I2 = that.cluster1
					.getBitSet().intersectionSize(other.cluster3.getBitSet()), I3 = that.cluster2
					.getBitSet().intersectionSize(other.cluster1.getBitSet()), I4 = that.cluster2
					.getBitSet().intersectionSize(other.cluster2.getBitSet()), I5 = that.cluster2
					.getBitSet().intersectionSize(other.cluster3.getBitSet()), I6 = that.cluster3
					.getBitSet().intersectionSize(other.cluster1.getBitSet()), I7 = that.cluster3
					.getBitSet().intersectionSize(other.cluster2.getBitSet()), I8 = that.cluster3
					.getBitSet().intersectionSize(other.cluster3.getBitSet());
			long sum = 0;
			return F(I0, I4, I8) + F(I0, I5, I7) + F(I1, I3, I8)+F(I1, I5, I6) + F(I2, I3, I7) + F(I2, I4, I6);//+
			
		}
	}

	public void useSetWeightsAlgorithm() {
		algorithm = new SetWeightCalculator();
	}

	/**
	 * obsolete (for now)
	 */
	public void preCalculateWeights(List<Tree> trees, List<Tree> extraTrees) {
	}

	/**
	 * Each algorithm will have its own data structure for gene trees
	 * 
	 * @param wqInference
	 */
	public void setupGeneTrees(WQInference wqInference) {
		//this.tmpalgorithm.setupGeneTrees(wqInference);
		//this.algorithm.setupGeneTrees(wqInference);
		tmpalgorithm.setupGeneTrees(wqInference);
		this.algorithm.setupGeneTrees(wqInference);
	}

	// TODO: this is algorithm-specific should not be exposed. Fix.
	public Integer[] geneTreesAsInts() {
		//return ((SetWeightCalculator)this.tmpalgorithm).geneTreesAsInts;
		return ((TraversalWeightCalculator)tmpalgorithm).geneTreesAsInts;
	}


}
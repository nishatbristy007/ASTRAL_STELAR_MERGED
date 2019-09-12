## DESCRIPTION:

STELAR is a Dynamic Programming (DP) based software for estimating species trees given a set of rooted gene trees, such that triplet consistency (between a species tree and the gene trees) is maximized within a constrained search space. STELAR is fast, highly accurate and statistically consistent. 
The algorithm used is described in our paper **"STELAR: A statistically consistent coalescent-based species trees estimation method by maximizing triplet consistency."** [(Paper link)](https://www.biorxiv.org/content/10.1101/594911v1)

## Availability

STELAR is freely available at [https://github.com/islamazhar/STELAR](https://islamazhar.github.io/STELAR/#installation). 
A step by step installation guide and other are also available there.

## Background and Results
Species tree estimation is frequently based on phylogenomic approaches that use multiple genes from throughout the genome. 
However, estimating a species tree from a collection of gene trees can be complicated due to the presence of gene tree incongruence resulting from incomplete lineage sorting (ILS), which is modelled by the multi-species coalescent process. 
Maximum likelihood and Bayesian MCMC methods can potentially result in accurate trees, but they do not scale well to large datasets.

We present STELAR (Species Tree Estimation by maximizing tripLet AgReement), a new fast and highly accurate statistically consistent coalescent-based method for estimating species trees from a collection of gene trees. 
We formalized the constrained triplet consensus (CTC) problem and showed that the solution to the CTC problem is a statistically consistent estimate of the species tree under the multi-species coalescent (MSC) model. 
STELAR is an efficient dynamic programming based solution to the CTC problem which is very fast and highly accurate. We evaluated the accuracy of STELAR in comparison with MP-EST and ASTRAL -- two of the most popular and accurate coalescent-based methods. 
Experimental results suggest that STELAR matches the accuracy of ASTRAL and improves on MP-EST.

Theoretical and empirical results (on both simulated and real biological datasets) suggest that 
STELAR is a valuable technique for species tree estimation from gene tree distributions.

## Acknowledgment
STELAR code uses code from the PhyloNet package by Luay Nakhleh (bioinfo.cs.rice.edu/phylonet). 
The Phylonet code base was previously used by DynaDup and ASTRAL (with permission from Authors), and 
STELAR is mostly based on DynaDup code base.

## Bug Reports
We are always looking for ways to improve our codes. For any bugs please email at: mazhar.buet11@gmail.com

# Large Scale Data Processing: Final Project
## Graph matching
Owen Lawlor, Ahmad Choudhry  

|           File name           |        Matching Size         |        Run Time              |  Matching?   |
| ------------------------------| ---------------------------- | ---------------------------- | ------------ |
| com-orkut.ungraph.csv         | 1268133                      |         433 seconds          | Yes, 1268133 |
| twitter_original_edges.csv    | 90723                        |         202 seconds          | Yes, 90596   | 
| soc-LiveJournal1.csv          | 1430096                      |         197 seconds          | Yes, 1430096 |
| soc-pokec-relationships.csv   | 576909                       |         98 seconds           | Yes, 576909  |
| musae_ENGB_edges.csv          | 2172                         |         9 seconds            | Yes, 2172    |
| log_normal_100.csv            | 48                           |         9 seconds            | Yes, 48      |

Each Program's results were gathered through the use of the Greedy Algorithm (explained later) on Google Cloud Platform. The setup had 1 x 2 N2 cores, one master and two worker nodes. Each node was given 8 gb of ram, as well as 250 gb disk storage for the master node, and 125 gb for each worker node. 

### Description of Approach
Our approach to obtaining these outputs relied on our specific implementation of the Greedy Algorithm for computing a maximal matching. The Greedy Algorithm, in its basic form, works by:
1.  Instantiating an empty list, M
2.  (Optional) Sort the graph based on a certain characteristic of each edge
3.  Iterate through the edges
4.  If the current edge is not in M, add it to M
5.  Else, continue
6.  Run until the last edge has been reached
7.  Return M

This was our original idea for obtaining the maximal (or as close as possible to maximal) outputs for each file using greedy. However, we wanted an algorithm that would scale well and work more efficiently if given more machines to process the information. Thus, our Greedy algorithm uses several partitions of the graph, adding them into local arrays before tallying them together in the overall matching list, "M". While the first run-through with this approach was very quick, especially given limited computational power, there would sometimes be overlap/spill-over between partitions that would cause the local matchings to be correct, while the overall matching list would be incorrect. This is because partitions did not consider the results of the other simultaneous partitions. To fix this we instituted a loop at the end of the code that goes through the matched edges list and eliminates the spill over between partitions by adding each entry to a new list, if and only if that edge's source and distance vertices were not already included in the new list. While this slowed our runtime a little bit, it guaranteed a correct matching that was still very close to the previous best results shown in class

If we were to do this again in the future with new test cases, I believe that our general strategy would be the same. However, I think to make the most ideal results, both in terms of maximality and runtime, we would need to find another solution to partition overlap and also have a more powerful pc. In addition, having multiple machines would help to more fully realize the speed up effects of partitioning.

Additionally, while we made attempts at using other algorithms, especially Luby's Algorithm, this algorithm was found to be the smoothest to implement, with good run times and surprisingly natural ways to parallelize the process.

### Algorithmic Advantages and Disadvantages

Another Difference between our algorithm and what the typical Greedy Algorithm may have is that we sort the graph before processing its vertices and edges. After researching the algorithm a bit, we found that sorting the algorithm can lead to larger and more accurate matchings, as it is easier for the greedy algorithm to traverse a sorted graph. While there are many ways to sort the graph, we decided to sort each each by their srcId's and dstId's, as it was an easy approach that worked well with the overall simplicity of the Greedy Algorithm. The disadvantage of sorting, however, is that it can lead to a larger runtime.

Typically the Greedy Algorithm has a runtime of O(E) where E is the number of edges, which would be the case in the earlier step-by-step pseudocode, without sorting. Since we decided to sort the edges first, this gave us a runtime of O(E * Log(E)). The other components of our function have a smaller runtime complexity. In total, the partitioning step has time complexity O(E), adding to the accumulators could have O(E) or O(V) time complexity in the worst case, and turning the lists into RDDs to be used in the returned graph has time complexity O(E) or O(V). Hence, the dominant runtime for this function is O(E * Log(E))

In terms of accuracy, the general greedy algorithm offers a 1/2-approximation of the maximum matching in a given graph, where the output of the greedy algorithm is at least 1/2 as large as the maximum matching. Since our algorithm is simply a parallel computing implementation of this greedy algorithm, the same should hold for our program. 

### Proof

Say M is a maximum matching and G is the matching obtained by the greedy algorithm. 

Suppose that the greedy algorithm chooses an edge e, on each iteration, where each vertex of e is not associated with any previous edge e in G. This edge is added to G and prevents at most 2 other edges in M are prevented from being added to G. 

Each edge has format (u,v), and some edge in M, w which is != v, that is connected to u by another edge and a vertex z which is != u, that is connected to v by another edge will be prevented from joining G. These are the two edges that cannot be added to G under the conditions of the greedy algorithm. 

Say |G| and |M| represent the sizes of G and M, respectively. 

Since each e in G stops at most 2 edges in M from being added to G, we have the inequality 2|G| >= |M|. In other words, G >= (1/2) * |M|, proving that the greedy algorithm provides a 1/2-approximation for M.

Sources: https://arxiv.org/pdf/2206.13057#:~:text=There%20is%20a%20simple%20greedy,a%201%2F2%2Dapproximation.
         https://www.cis.upenn.edu/~aaroth/courses/slides/privacymd/Lecture7.pdf
         
We also tried implementing lubys algorithm but the code was not efficient enough to run with larger data sets. The code for lubys is in a file called lubys in the main directory but it is not in main as we did not get it working for the larger datasets.
  
 

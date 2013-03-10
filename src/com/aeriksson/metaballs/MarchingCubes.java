package com.aeriksson.metaballs;

/**
 * Simple implementation of the Marching Cubes algorithm.
 * 
 * Renders the isosurfaces of a scalar field by sampling the field at a uniform
 * grid, and constructing triangles inside each cube of the grid based on the
 * function signs at its corners. Uses a lookup table containing triangle
 * specifications for each possible combination of corner values. See
 * http://en.wikipedia.org/wiki/Marching_cubes for more info.
 * 
 * The class supports linear interpolation of function values, computation of
 * surface normals, and custom colors through a separate color scalar field.
 * 
 * Note: this is an implementation of the original MC algorithm, which is
 * unfortunately not able to always correctly represent the isosurface topology.
 * This means that holes occasionally show up in the output mesh.
 * 
 * For any serious use this issue must be mitigated. Fortunately, this can be
 * done with little work. See
 * http://users.polytech.unice.fr/~lingrand/MarchingCubes/algo.html for more
 * information on the issue and how to resolve it. This class could be modified
 * to automatically solve ambiguities by deferring the triangle computation for
 * any ambiguous cubes until after all other cubes have been rendered, and
 * checking the vertexCache for which triangle combination should be used.
 * 
 * The corners and edges are indexed as follows:
 * 
 * Corner indices:..............................................
 * .....6______________7........................................
 * ..../|............./|........................................
 * .../.|............/.|........................................
 * ../..|.........../..|........................................
 * .4______________5 ..|........................................
 * .|...|..........|...|........................................
 * .|...|..........|...|........................................
 * .|...2__________|___3........................................
 * .|../...........|../.........................................
 * .|./............|./..........................................
 * .|/.............|/...........................................
 * .0______________1............................................
 * 
 * Edge indices:................................................
 * .....______10_______.........................................
 * ..../|............./|........................................
 * ..11.|............9.|........................................
 * ../..|.........../..|........................................
 * ./______8_______/ ..|........................................
 * .|...6..........|...5........................................
 * .|...|..........|...|........................................
 * .|...|______2___|___|........................................
 * .7../...........4../.........................................
 * .|.3............|.1..........................................
 * .|/.............|/...........................................
 * ./______0_______/............................................
 * 
 * The standard right-handed Cartesian coordinate system is assumed:
 * ..............................................................
 * .z............................................................
 * .^............................................................
 * .|...^.y......................................................
 * .|../.........................................................
 * .|./..........................................................
 * .K_____>.x....................................................
 * ..............................................................
 */
public class MarchingCubes {

	/**
	 * Number of triangles in each element of TRIANGLE_SPECIFICATIONS.
	 * Technically superfluous right now, but can be useful if porting the code
	 * to C.
	 */
	private final static int[] TRIANGLE_COUNTS = { 0, 1, 1, 2, 1, 2, 2, 3, 1,
			2, 2, 3, 2, 3, 3, 2, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4,
			3, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 3, 2, 3, 3, 2, 3,
			4, 4, 3, 3, 4, 4, 3, 4, 3, 3, 2, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3,
			4, 3, 4, 4, 3, 2, 3, 3, 4, 3, 2, 4, 3, 3, 4, 4, 3, 4, 3, 3, 2, 2,
			3, 3, 4, 3, 4, 4, 3, 3, 4, 4, 3, 4, 3, 3, 2, 3, 4, 4, 3, 4, 3, 3,
			2, 4, 3, 3, 2, 3, 2, 2, 1, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3,
			4, 4, 3, 2, 3, 3, 4, 3, 4, 4, 3, 3, 4, 4, 3, 4, 3, 3, 2, 2, 3, 3,
			4, 3, 4, 4, 3, 3, 4, 2, 3, 4, 3, 3, 2, 3, 4, 4, 3, 4, 3, 3, 2, 4,
			3, 3, 2, 3, 2, 2, 1, 2, 3, 3, 4, 3, 4, 4, 3, 3, 4, 4, 3, 2, 3, 3,
			2, 3, 4, 4, 3, 4, 3, 3, 2, 4, 3, 3, 2, 3, 2, 2, 1, 3, 4, 4, 3, 4,
			3, 3, 2, 4, 3, 3, 2, 3, 2, 2, 1, 2, 3, 3, 2, 3, 2, 2, 1, 3, 2, 2,
			1, 2, 1, 1, 0 };

	/**
	 * Array containing, for each possible combination of cube corner values,
	 * the corresponding triangles as edge indices. Indexed by an eight-bit
	 * integer where bit number i is 1 if corner i has a positive sign and 0
	 * otherwise.
	 */
	final static int[][] TRIANGLE_SPECIFICATIONS = { {}, { 0, 3, 7 },
			{ 0, 1, 4 }, { 4, 1, 7, 7, 1, 3 }, { 6, 2, 3 },
			{ 0, 7, 6, 0, 6, 2 }, { 6, 2, 3, 4, 0, 1 },
			{ 2, 6, 1, 1, 6, 4, 4, 6, 7 }, { 5, 2, 1 }, { 3, 7, 0, 1, 5, 2 },
			{ 2, 5, 4, 2, 4, 0 }, { 7, 3, 2, 7, 2, 5, 7, 5, 4 },
			{ 5, 1, 3, 5, 3, 6 }, { 5, 1, 0, 5, 0, 7, 5, 7, 6 },
			{ 0, 4, 3, 3, 4, 6, 6, 4, 5 }, { 4, 5, 7, 7, 5, 6 }, { 7, 8, 11 },
			{ 11, 8, 3, 3, 8, 0 }, { 11, 7, 8, 1, 4, 0 },
			{ 4, 1, 8, 8, 1, 11, 11, 1, 3 }, { 2, 3, 6, 8, 11, 7 },
			{ 11, 8, 6, 6, 8, 2, 2, 8, 0 }, { 7, 11, 8, 1, 0, 4, 3, 2, 6 },
			{ 1, 4, 8, 1, 8, 2, 2, 8, 11, 2, 11, 6 }, { 7, 8, 11, 5, 1, 2 },
			{ 3, 0, 8, 3, 8, 11, 5, 1, 2 }, { 0, 4, 2, 2, 4, 5, 7, 11, 8 },
			{ 4, 5, 2, 4, 2, 11, 4, 11, 8, 11, 2, 3 },
			{ 6, 3, 5, 5, 3, 1, 11, 8, 7 },
			{ 6, 5, 1, 6, 1, 8, 8, 11, 6, 0, 8, 1 },
			{ 0, 4, 3, 3, 4, 6, 6, 4, 5, 11, 8, 7 },
			{ 6, 11, 8, 6, 8, 4, 6, 4, 5 }, { 4, 8, 9 }, { 0, 3, 7, 8, 9, 4 },
			{ 1, 0, 9, 9, 0, 8 }, { 9, 8, 7, 9, 7, 3, 9, 3, 1 },
			{ 6, 2, 3, 4, 9, 8 }, { 0, 7, 6, 0, 6, 2, 9, 8, 4 },
			{ 1, 0, 9, 9, 0, 8, 2, 6, 3 },
			{ 9, 1, 2, 9, 2, 7, 7, 8, 9, 6, 7, 2 }, { 5, 2, 1, 4, 8, 9 },
			{ 5, 1, 2, 0, 7, 3, 8, 4, 9 }, { 2, 5, 9, 2, 9, 8, 2, 8, 0 },
			{ 9, 5, 2, 9, 2, 8, 8, 2, 3, 8, 3, 7 },
			{ 5, 1, 3, 5, 3, 6, 8, 4, 9 },
			{ 5, 1, 0, 5, 0, 7, 5, 7, 6, 9, 8, 4 },
			{ 0, 8, 9, 0, 9, 6, 0, 6, 3, 6, 9, 5 },
			{ 8, 7, 9, 9, 7, 5, 5, 7, 6 }, { 4, 9, 11, 4, 11, 7 },
			{ 3, 0, 4, 3, 4, 9, 3, 9, 11 }, { 7, 11, 0, 0, 11, 1, 1, 11, 9 },
			{ 9, 1, 3, 9, 3, 11 }, { 7, 11, 4, 4, 11, 9, 3, 2, 6 },
			{ 0, 2, 4, 4, 2, 11, 11, 2, 6, 4, 11, 9 },
			{ 1, 2, 6, 1, 6, 11, 1, 11, 9, 0, 7, 3 },
			{ 1, 2, 6, 1, 6, 11, 1, 11, 9 }, { 4, 9, 11, 4, 11, 7, 2, 5, 1 },
			{ 5, 9, 2, 2, 9, 3, 3, 9, 11, 0, 4, 1 },
			{ 9, 11, 5, 5, 11, 0, 7, 0, 11, 0, 2, 5 },
			{ 5, 9, 2, 2, 9, 3, 3, 9, 11 },
			{ 7, 4, 9, 7, 9, 11, 5, 1, 3, 5, 3, 6 },
			{ 5, 9, 6, 6, 9, 11, 1, 0, 4 }, { 6, 11, 9, 6, 9, 5, 0, 7, 3 },
			{ 5, 9, 6, 6, 9, 11 }, { 10, 11, 6 }, { 6, 10, 11, 7, 0, 3 },
			{ 10, 11, 6, 4, 1, 0 }, { 7, 3, 1, 7, 1, 4, 10, 6, 11 },
			{ 3, 2, 11, 11, 2, 10 }, { 0, 7, 11, 0, 11, 10, 0, 10, 2 },
			{ 3, 2, 11, 11, 2, 10, 0, 4, 1 },
			{ 10, 2, 1, 10, 1, 7, 10, 7, 11, 7, 1, 4 }, { 10, 11, 6, 2, 1, 5 },
			{ 7, 3, 0, 2, 5, 1, 10, 6, 11 }, { 2, 5, 4, 2, 4, 0, 11, 10, 6 },
			{ 10, 5, 11, 11, 5, 7, 7, 5, 4, 3, 2, 6 },
			{ 11, 10, 5, 11, 5, 1, 11, 1, 3 },
			{ 1, 5, 0, 0, 5, 10, 0, 10, 7, 7, 10, 11 },
			{ 4, 5, 0, 0, 5, 11, 10, 11, 5, 11, 3, 0 },
			{ 10, 5, 11, 11, 5, 7, 7, 5, 4 }, { 10, 6, 7, 10, 7, 8 },
			{ 10, 6, 3, 10, 3, 0, 10, 0, 8 }, { 8, 7, 10, 10, 7, 6, 4, 1, 0 },
			{ 1, 3, 4, 4, 3, 10, 6, 10, 3, 10, 8, 4 },
			{ 3, 2, 7, 7, 2, 8, 8, 2, 10 }, { 2, 0, 8, 2, 8, 10 },
			{ 3, 2, 7, 7, 2, 8, 8, 2, 10, 4, 1, 0 },
			{ 8, 4, 1, 8, 1, 2, 8, 2, 10 }, { 10, 6, 7, 10, 7, 8, 1, 2, 5 },
			{ 10, 6, 3, 10, 3, 0, 10, 0, 8, 5, 1, 2 },
			{ 2, 0, 7, 2, 7, 6, 8, 4, 5, 8, 5, 10 },
			{ 10, 5, 8, 8, 5, 4, 6, 3, 2 },
			{ 10, 8, 5, 5, 8, 3, 3, 8, 7, 5, 3, 1 },
			{ 1, 0, 5, 5, 0, 10, 10, 0, 8 }, { 8, 4, 5, 8, 5, 10, 3, 0, 7 },
			{ 8, 4, 5, 8, 5, 10 }, { 11, 6, 10, 9, 4, 8 },
			{ 6, 11, 10, 8, 4, 9, 0, 7, 3 }, { 9, 8, 0, 9, 0, 1, 6, 11, 10 },
			{ 9, 8, 7, 9, 7, 3, 9, 3, 1, 10, 6, 11 },
			{ 11, 10, 2, 11, 2, 3, 4, 9, 8 },
			{ 0, 7, 11, 0, 11, 10, 0, 10, 2, 4, 9, 8 },
			{ 8, 0, 11, 11, 0, 3, 9, 10, 1, 1, 10, 2 },
			{ 9, 10, 1, 1, 10, 2, 8, 7, 11 }, { 4, 9, 8, 10, 6, 11, 2, 5, 1 },
			{ 0, 3, 7, 1, 5, 2, 8, 9, 4, 10, 11, 6 },
			{ 11, 8, 6, 6, 8, 2, 2, 8, 0, 5, 9, 10 },
			{ 6, 3, 2, 9, 10, 5, 11, 8, 7 },
			{ 4, 1, 8, 8, 1, 11, 11, 1, 3, 10, 5, 9 },
			{ 4, 1, 0, 11, 8, 7, 9, 10, 5 }, { 11, 8, 3, 3, 8, 0, 10, 5, 9 },
			{ 7, 8, 11, 5, 10, 9 }, { 4, 9, 10, 4, 10, 6, 4, 6, 7 },
			{ 9, 4, 10, 10, 4, 0, 10, 0, 6, 6, 0, 3 },
			{ 1, 9, 0, 0, 9, 6, 6, 9, 10, 0, 6, 7 },
			{ 6, 3, 10, 10, 3, 9, 9, 3, 1 },
			{ 7, 4, 9, 7, 9, 2, 2, 3, 7, 10, 2, 9 },
			{ 9, 10, 4, 4, 10, 0, 0, 10, 2 }, { 1, 2, 10, 1, 10, 9, 7, 3, 0 },
			{ 9, 10, 1, 1, 10, 2 }, { 4, 9, 10, 4, 10, 6, 4, 6, 7, 1, 2, 5 },
			{ 5, 9, 10, 3, 2, 6, 1, 0, 4 }, { 2, 6, 0, 0, 6, 7, 5, 9, 10 },
			{ 3, 6, 2, 9, 5, 10 }, { 4, 1, 7, 7, 1, 3, 9, 10, 5 },
			{ 0, 1, 4, 10, 9, 5 }, { 0, 3, 7, 5, 9, 10 }, { 10, 9, 5 },
			{ 10, 9, 5 }, { 0, 3, 7, 5, 9, 10 }, { 0, 1, 4, 10, 9, 5 },
			{ 4, 1, 7, 7, 1, 3, 9, 10, 5 }, { 3, 6, 2, 9, 5, 10 },
			{ 2, 6, 0, 0, 6, 7, 5, 9, 10 }, { 5, 9, 10, 3, 2, 6, 1, 0, 4 },
			{ 4, 9, 10, 4, 10, 6, 4, 6, 7, 1, 2, 5 }, { 9, 10, 1, 1, 10, 2 },
			{ 1, 2, 10, 1, 10, 9, 7, 3, 0 }, { 9, 10, 4, 4, 10, 0, 0, 10, 2 },
			{ 7, 4, 9, 7, 9, 2, 2, 3, 7, 10, 2, 9 },
			{ 6, 3, 10, 10, 3, 9, 9, 3, 1 },
			{ 1, 9, 0, 0, 9, 6, 6, 9, 10, 0, 6, 7 },
			{ 9, 4, 10, 10, 4, 0, 10, 0, 6, 6, 0, 3 },
			{ 4, 9, 10, 4, 10, 6, 4, 6, 7 }, { 7, 8, 11, 5, 10, 9 },
			{ 11, 8, 3, 3, 8, 0, 10, 5, 9 }, { 4, 1, 0, 11, 8, 7, 9, 10, 5 },
			{ 4, 1, 8, 8, 1, 11, 11, 1, 3, 10, 5, 9 },
			{ 6, 3, 2, 9, 10, 5, 11, 8, 7 },
			{ 11, 8, 6, 6, 8, 2, 2, 8, 0, 5, 9, 10 },
			{ 0, 3, 7, 1, 5, 2, 8, 9, 4, 10, 11, 6 },
			{ 4, 9, 8, 10, 6, 11, 2, 5, 1 }, { 9, 10, 1, 1, 10, 2, 8, 7, 11 },
			{ 8, 0, 11, 11, 0, 3, 9, 10, 1, 1, 10, 2 },
			{ 0, 7, 11, 0, 11, 10, 0, 10, 2, 4, 9, 8 },
			{ 11, 10, 2, 11, 2, 3, 4, 9, 8 },
			{ 9, 8, 7, 9, 7, 3, 9, 3, 1, 10, 6, 11 },
			{ 9, 8, 0, 9, 0, 1, 6, 11, 10 }, { 6, 11, 10, 8, 4, 9, 0, 7, 3 },
			{ 11, 6, 10, 9, 4, 8 }, { 8, 4, 5, 8, 5, 10 },
			{ 8, 4, 5, 8, 5, 10, 3, 0, 7 }, { 1, 0, 5, 5, 0, 10, 10, 0, 8 },
			{ 10, 8, 5, 5, 8, 3, 3, 8, 7, 5, 3, 1 },
			{ 10, 5, 8, 8, 5, 4, 6, 3, 2 },
			{ 2, 0, 7, 2, 7, 6, 8, 4, 5, 8, 5, 10 },
			{ 10, 6, 3, 10, 3, 0, 10, 0, 8, 5, 1, 2 },
			{ 10, 6, 7, 10, 7, 8, 1, 2, 5 }, { 8, 4, 1, 8, 1, 2, 8, 2, 10 },
			{ 3, 2, 7, 7, 2, 8, 8, 2, 10, 4, 1, 0 }, { 2, 0, 8, 2, 8, 10 },
			{ 3, 2, 7, 7, 2, 8, 8, 2, 10 },
			{ 1, 3, 4, 4, 3, 10, 6, 10, 3, 10, 8, 4 },
			{ 8, 7, 10, 10, 7, 6, 4, 1, 0 }, { 10, 6, 3, 10, 3, 0, 10, 0, 8 },
			{ 10, 6, 7, 10, 7, 8 }, { 10, 5, 11, 11, 5, 7, 7, 5, 4 },
			{ 4, 5, 0, 0, 5, 11, 10, 11, 5, 11, 3, 0 },
			{ 1, 5, 0, 0, 5, 10, 0, 10, 7, 7, 10, 11 },
			{ 11, 10, 5, 11, 5, 1, 11, 1, 3 },
			{ 10, 5, 11, 11, 5, 7, 7, 5, 4, 3, 2, 6 },
			{ 2, 5, 4, 2, 4, 0, 11, 10, 6 }, { 7, 3, 0, 2, 5, 1, 10, 6, 11 },
			{ 10, 11, 6, 2, 1, 5 }, { 10, 2, 1, 10, 1, 7, 10, 7, 11, 7, 1, 4 },
			{ 3, 2, 11, 11, 2, 10, 0, 4, 1 },
			{ 0, 7, 11, 0, 11, 10, 0, 10, 2 }, { 3, 2, 11, 11, 2, 10 },
			{ 7, 3, 1, 7, 1, 4, 10, 6, 11 }, { 10, 11, 6, 4, 1, 0 },
			{ 6, 10, 11, 7, 0, 3 }, { 10, 11, 6 }, { 5, 9, 6, 6, 9, 11 },
			{ 6, 11, 9, 6, 9, 5, 0, 7, 3 }, { 5, 9, 6, 6, 9, 11, 1, 0, 4 },
			{ 7, 4, 9, 7, 9, 11, 5, 1, 3, 5, 3, 6 },
			{ 5, 9, 2, 2, 9, 3, 3, 9, 11 },
			{ 9, 11, 5, 5, 11, 0, 7, 0, 11, 0, 2, 5 },
			{ 5, 9, 2, 2, 9, 3, 3, 9, 11, 0, 4, 1 },
			{ 4, 9, 11, 4, 11, 7, 2, 5, 1 }, { 1, 2, 6, 1, 6, 11, 1, 11, 9 },
			{ 1, 2, 6, 1, 6, 11, 1, 11, 9, 0, 7, 3 },
			{ 0, 2, 4, 4, 2, 11, 11, 2, 6, 4, 11, 9 },
			{ 7, 11, 4, 4, 11, 9, 3, 2, 6 }, { 9, 1, 3, 9, 3, 11 },
			{ 7, 11, 0, 0, 11, 1, 1, 11, 9 }, { 3, 0, 4, 3, 4, 9, 3, 9, 11 },
			{ 4, 9, 11, 4, 11, 7 }, { 8, 7, 9, 9, 7, 5, 5, 7, 6 },
			{ 0, 8, 9, 0, 9, 6, 0, 6, 3, 6, 9, 5 },
			{ 5, 1, 0, 5, 0, 7, 5, 7, 6, 9, 8, 4 },
			{ 5, 1, 3, 5, 3, 6, 8, 4, 9 },
			{ 9, 5, 2, 9, 2, 8, 8, 2, 3, 8, 3, 7 },
			{ 2, 5, 9, 2, 9, 8, 2, 8, 0 }, { 5, 1, 2, 0, 7, 3, 8, 4, 9 },
			{ 5, 2, 1, 4, 8, 9 }, { 9, 1, 2, 9, 2, 7, 7, 8, 9, 6, 7, 2 },
			{ 1, 0, 9, 9, 0, 8, 2, 6, 3 }, { 0, 7, 6, 0, 6, 2, 9, 8, 4 },
			{ 6, 2, 3, 4, 9, 8 }, { 9, 8, 7, 9, 7, 3, 9, 3, 1 },
			{ 1, 0, 9, 9, 0, 8 }, { 0, 3, 7, 8, 9, 4 }, { 4, 8, 9 },
			{ 6, 11, 8, 6, 8, 4, 6, 4, 5 },
			{ 0, 4, 3, 3, 4, 6, 6, 4, 5, 11, 8, 7 },
			{ 6, 5, 1, 6, 1, 8, 8, 11, 6, 0, 8, 1 },
			{ 6, 3, 5, 5, 3, 1, 11, 8, 7 },
			{ 4, 5, 2, 4, 2, 11, 4, 11, 8, 11, 2, 3 },
			{ 0, 4, 2, 2, 4, 5, 7, 11, 8 }, { 3, 0, 8, 3, 8, 11, 5, 1, 2 },
			{ 7, 8, 11, 5, 1, 2 }, { 1, 4, 8, 1, 8, 2, 2, 8, 11, 2, 11, 6 },
			{ 7, 11, 8, 1, 0, 4, 3, 2, 6 }, { 11, 8, 6, 6, 8, 2, 2, 8, 0 },
			{ 2, 3, 6, 8, 11, 7 }, { 4, 1, 8, 8, 1, 11, 11, 1, 3 },
			{ 11, 7, 8, 1, 4, 0 }, { 11, 8, 3, 3, 8, 0 }, { 7, 8, 11 },
			{ 4, 5, 7, 7, 5, 6 }, { 0, 4, 3, 3, 4, 6, 6, 4, 5 },
			{ 5, 1, 0, 5, 0, 7, 5, 7, 6 }, { 5, 1, 3, 5, 3, 6 },
			{ 7, 3, 2, 7, 2, 5, 7, 5, 4 }, { 2, 5, 4, 2, 4, 0 },
			{ 3, 7, 0, 1, 5, 2 }, { 5, 2, 1 }, { 2, 6, 1, 1, 6, 4, 4, 6, 7 },
			{ 6, 2, 3, 4, 0, 1 }, { 0, 7, 6, 0, 6, 2 }, { 6, 2, 3 },
			{ 4, 1, 7, 7, 1, 3 }, { 0, 1, 4 }, { 0, 3, 7 }, {} };

	/**
	 * Masks used to compress the function signs at cube corners into integers.
	 * This lets us use these signs as indexes in TRIANGLE_SPECIFICATIONS.
	 */
	private final static int[] CORNER_MASKS = { 1, 1 << 1, 1 << 2, 1 << 3,
			1 << 4, 1 << 5, 1 << 6, 1 << 7 };

	/**
	 * The maximum number of triangles in the resulting mesh.
	 */
	private static final int TRIANGLE_CAPACITY = 100000;

	/**
	 * The number of floating point values per vertex. Currently three each for
	 * position, normals and color.
	 */
	private static final int FLOATS_PER_VERTEX = 9;
	private static final int FLOATS_PER_TRIANGLE = FLOATS_PER_VERTEX * 3;

	private static final float[] DEFAULT_CLIPPING_BOX = { -2.5f, 2.5f,
			-2.5f, 2.5f, -2.5f, 2.5f };
	private static final int DEFAULT_ELEMENTS_PER_SIDE = 20;

	/** Step value for computing partial derivatives of the scalar field */
	private static final float DERIVATIVE_DELTA_CONSTANT = 1e-4f;

	/** The volume being rendered, as [xMin, xMax, yMin, yMax, zMin, zMax]. */
	private final float[] computationVolume;

	/**
	 * Number of elements per side of the viewing volume
	 */
	private final int elementsPerSide;

	/**
	 * Dimensions of individual elements as x/y/z widths.
	 */
	private final float[] elementDimensions;

	/** The scalar field being rendered */
	private ScalarField function;

	/** Scalar field used to compute vertex colors */
	private VectorField colorField;

	/**
	 * Buffer for function values at cube corner points Indexed as [x][y][z].
	 */
	private float[][][] functionValues;

	/**
	 * Cache containing all computed vertices. Each vertex is shared by at least
	 * four triangles, so having this lets us eliminate a lot of function
	 * evaluations. Indexed as [x][y][z][i], where x/y/z are the cube indices,
	 * and i is the edge index. Since there are three unique edges per cube, i
	 * takes on the values 0, 1 or 2, corresponding to the edges 0, 3 and 7:
	 * ....._______________....................................
	 * ..../|............./|...................................
	 * .../.|............/.|...................................
	 * ../..|.........../..|...................................
	 * ./______________/ ..|...................................
	 * .|...|..........|...|...................................
	 * .|...|..........|...|...................................
	 * .2...|__________|___|...................................
	 * .|../...........|../....................................
	 * .|.1............|./.....................................
	 * .|/.............|/......................................
	 * ./______0_______/.......................................
	 */
	private int[][][][] vertexCache;

	/**
	 * Buffer for the computed triangles. Vertices, normals and color values are
	 * currently interleaved, so that each vertex uses 9 floats.
	 * 
	 * TODO: This is retarded. Split it into three separate buffers.
	 */
	float[] triangleList;

	/** Index of the first non-allocated element in triangleList */
	private int triangleListPosition = 0;

	/** Triangles in buffer */
	private int triangleCount = 0;

	public MarchingCubes() {
		this(DEFAULT_CLIPPING_BOX, DEFAULT_ELEMENTS_PER_SIDE);
	}

	public MarchingCubes(float[] clippingBox, int elementsPerSide) {
		this.computationVolume = clippingBox;
		this.elementsPerSide = elementsPerSide;

		elementDimensions = new float[3];
		elementDimensions[0] = (clippingBox[1] - clippingBox[0])
				/ elementsPerSide;
		elementDimensions[1] = (clippingBox[3] - clippingBox[2])
				/ elementsPerSide;
		elementDimensions[2] = (clippingBox[5] - clippingBox[4])
				/ elementsPerSide;

		int n = elementsPerSide + 1;
		functionValues = new float[n][n][n];
		vertexCache = new int[n][n][n][3];
		triangleList = new float[TRIANGLE_CAPACITY * FLOATS_PER_TRIANGLE];
	}

	/** Clears all buffers */
	private void reset() {
		int i, j, k, n;
		n = elementsPerSide + 1;
		for (i = 0; i < n; i++) {
			for (j = 0; j < n; j++) {
				for (k = 0; k < n; k++) {
					functionValues[i][j][k] = 0;
					vertexCache[i][j][k][0] = 0;
					vertexCache[i][j][k][1] = 0;
					vertexCache[i][j][k][2] = 0;
				}
			}
		}
		for (i = 0; i < triangleListPosition; i++) {
			triangleList[i] = 0;
		}
		
		triangleCount = 0;
		triangleListPosition = 0;
	}

	public void compute() {
		reset();

		computeGridValues();
		computeTriangles();
	}

	public void setFunction(ScalarField function) {
		this.function = function;
	}

	public void setColorField(VectorField colorField) {
		this.colorField = colorField;
	}

	public float[] getTriangles() {
		return triangleList;
	}

	public int getTriangleCount() {
		return triangleCount;
	}

	private void computeGridValues() {
		final float xMin = computationVolume[0];
		final float yMin = computationVolume[2];
		final float zMin = computationVolume[4];
		float x, y, z;
		int i, j, k;
		for (i = 0, x = xMin; i <= elementsPerSide; i++, x += elementDimensions[0]) {
			for (j = 0, y = yMin; j <= elementsPerSide; j++, y += elementDimensions[1]) {
				for (k = 0, z = zMin; k <= elementsPerSide; k++, z += elementDimensions[2]) {
					functionValues[i][j][k] = function.evaluate(x, y, z);
				}
			}
		}
	}

	private void computeTriangles() {
		int x, y, z, flags;
		for (x = 0; x < elementsPerSide; x++) {
			for (y = 0; y < elementsPerSide; y++) {
				for (z = 0; z < elementsPerSide; z++) {

					flags = getCornerFlags(x, y, z);

					// do nothing for cubes where the corner signs
					// are identical
					if (flags != 0 && flags != 255) {

						for (int i = 0; i < TRIANGLE_COUNTS[flags]; i++) {
							setTriangleFromSpecification(x, y, z, flags,
									TRIANGLE_SPECIFICATIONS[flags], i * 3);
						}
					}
				}
			}
		}
	}

	private void setTriangleFromSpecification(int x, int y, int z, int mask,
			int[] triangleSpec, int specOffset) {

		for (int i = 0; i < 3; i++) {

			final int edgeIndex = triangleSpec[specOffset + i];

			int[] cacheIndices = getCacheIndices(x, y, z, edgeIndex);

			int floatsIndex = getCachedFloats(cacheIndices);
			if (floatsIndex == 0) {
				int vertexIndex = setVertex(x, y, z, edgeIndex);
				setNormal(vertexIndex);
				setColor(vertexIndex);
				setCachedFloats(cacheIndices);
			} else {
				triangleList[triangleListPosition] = triangleList[floatsIndex];
				triangleList[triangleListPosition + 1] = triangleList[floatsIndex + 1];
				triangleList[triangleListPosition + 2] = triangleList[floatsIndex + 2];
				triangleList[triangleListPosition + 3] = triangleList[floatsIndex + 3];
				triangleList[triangleListPosition + 4] = triangleList[floatsIndex + 4];
				triangleList[triangleListPosition + 5] = triangleList[floatsIndex + 5];
				triangleList[triangleListPosition + 6] = triangleList[floatsIndex + 6];
				triangleList[triangleListPosition + 7] = triangleList[floatsIndex + 7];
				triangleList[triangleListPosition + 8] = triangleList[floatsIndex + 8];
			}
			triangleListPosition += 9;
			triangleCount++;
		}
	}

	private void setNormal(int vertexIndex) {
		float[] normal = computeGradientDirection(triangleList[vertexIndex],
				triangleList[vertexIndex + 1], triangleList[vertexIndex + 2]);

		triangleList[vertexIndex + 3] = normal[0];
		triangleList[vertexIndex + 4] = normal[1];
		triangleList[vertexIndex + 5] = normal[2];
	}

	private void setColor(int vertexIndex) {
		float[] color = colorField.evaluate(triangleList[vertexIndex],
				triangleList[vertexIndex + 1], triangleList[vertexIndex + 2]);

		triangleList[vertexIndex + 6] = color[0];
		triangleList[vertexIndex + 7] = color[1];
		triangleList[vertexIndex + 8] = color[2];
	}

	private int setVertex(int x, int y, int z, int edgeIndex) {
		final float xMin = computationVolume[0] + x * elementDimensions[0];
		final float xMax = xMin + elementDimensions[0];
		final float yMin = computationVolume[2] + y * elementDimensions[1];
		final float yMax = yMin + elementDimensions[1];
		final float zMin = computationVolume[4] + z * elementDimensions[2];
		final float zMax = zMin + elementDimensions[2];

		// linearly interpolate based on the corner values
		// TODO: use a lookup instead
		float v0, v1, v2, intercept;
		v0 = v1 = v2 = 0;
		switch (edgeIndex) {
		case 0:
			intercept = computeLinearIntercept(functionValues[x][y][z],
					functionValues[x + 1][y][z]);
			v0 = xMin + (xMax - xMin) * intercept;
			v1 = yMin;
			v2 = zMin;
			break;
		case 1:
			intercept = computeLinearIntercept(functionValues[x + 1][y][z],
					functionValues[x + 1][y + 1][z]);
			v0 = xMax;
			v1 = yMin + (yMax - yMin) * intercept;
			v2 = zMin;
			break;
		case 2:
			intercept = computeLinearIntercept(functionValues[x][y + 1][z],
					functionValues[x + 1][y + 1][z]);
			v0 = xMin + (xMax - xMin) * intercept;
			v1 = yMax;
			v2 = zMin;
			break;
		case 3:
			intercept = computeLinearIntercept(functionValues[x][y][z],
					functionValues[x][y + 1][z]);
			v0 = xMin;
			v1 = yMin + (yMax - yMin) * intercept;
			v2 = zMin;
			break;
		case 4:
			intercept = computeLinearIntercept(functionValues[x + 1][y][z],
					functionValues[x + 1][y][z + 1]);
			v0 = xMax;
			v1 = yMin;
			v2 = zMin + (zMax - zMin) * intercept;
			break;
		case 5:
			intercept = computeLinearIntercept(functionValues[x + 1][y + 1][z],
					functionValues[x + 1][y + 1][z + 1]);
			v0 = xMax;
			v1 = yMax;
			v2 = zMin + (zMax - zMin) * intercept;
			break;
		case 6:
			intercept = computeLinearIntercept(functionValues[x][y + 1][z],
					functionValues[x][y + 1][z + 1]);
			v0 = xMin;
			v1 = yMax;
			v2 = zMin + (zMax - zMin) * intercept;
			break;
		case 7:
			intercept = computeLinearIntercept(functionValues[x][y][z],
					functionValues[x][y][z + 1]);
			v0 = xMin;
			v1 = yMin;
			v2 = zMin + (zMax - zMin) * intercept;
			break;
		case 8:
			intercept = computeLinearIntercept(functionValues[x][y][z + 1],
					functionValues[x + 1][y][z + 1]);
			v0 = xMin + (xMax - xMin) * intercept;
			v1 = yMin;
			v2 = zMax;
			break;
		case 9:
			intercept = computeLinearIntercept(functionValues[x + 1][y][z + 1],
					functionValues[x + 1][y + 1][z + 1]);
			v0 = xMax;
			v1 = yMin + (yMax - yMin) * intercept;
			v2 = zMax;
			break;
		case 10:
			intercept = computeLinearIntercept(functionValues[x][y + 1][z + 1],
					functionValues[x + 1][y + 1][z + 1]);
			v0 = xMin + (xMax - xMin) * intercept;
			v1 = yMax;
			v2 = zMax;
			break;
		case 11:
			intercept = computeLinearIntercept(functionValues[x][y][z + 1],
					functionValues[x][y + 1][z + 1]);
			v0 = xMin;
			v1 = yMin + (yMax - yMin) * intercept;
			v2 = zMax;
			break;
		}

		triangleList[triangleListPosition] = v0;
		triangleList[triangleListPosition + 1] = v1;
		triangleList[triangleListPosition + 2] = v2;

		return triangleListPosition;
	}

	/**
	 * Computes the index in vertexCache for the given edge of the given
	 * element.
	 */
	private int[] getCacheIndices(int x, int y, int z, int edgeIndex) {
		int direction = 0;
		int xIndex = x;
		int yIndex = y;
		int zIndex = z;
		switch (edgeIndex) {
		case 0:
			direction = 0;
			break;
		case 1:
			xIndex += 1;
			direction = 1;
			break;
		case 2:
			yIndex += 1;
			direction = 0;
			break;
		case 3:
			direction = 1;
			break;
		case 4:
			xIndex += 1;
			direction = 2;
			break;
		case 5:
			xIndex += 1;
			yIndex += 1;
			direction = 2;
			break;
		case 6:
			yIndex += 1;
			direction = 2;
			break;
		case 7:
			direction = 2;
			break;
		case 8:
			zIndex += 1;
			direction = 0;
			break;
		case 9:
			xIndex += 1;
			zIndex += 1;
			direction = 1;
			break;
		case 10:
			zIndex += 1;
			yIndex += 1;
			direction = 0;
			break;
		case 11:
			zIndex += 1;
			direction = 1;
		}
		return new int[] { xIndex, yIndex, zIndex, direction };
	}

	private int getCachedFloats(int[] cacheIndices) {
		return vertexCache[cacheIndices[0]][cacheIndices[1]][cacheIndices[2]][cacheIndices[3]];
	}

	/**
	 * Sets the given vertex cache to the current triangle list position.
	 */
	private void setCachedFloats(int[] cacheIndices) {
		vertexCache[cacheIndices[0]][cacheIndices[1]][cacheIndices[2]][cacheIndices[3]] = triangleListPosition;
	}

	private int getCornerFlags(int x, int y, int z) {
		int flags = 0;
		if (functionValues[x][y][z] > 0) {
			flags |= CORNER_MASKS[0];
		}
		if (functionValues[x + 1][y][z] > 0) {
			flags |= CORNER_MASKS[1];
		}
		if (functionValues[x][y + 1][z] > 0) {
			flags |= CORNER_MASKS[2];
		}
		if (functionValues[x + 1][y + 1][z] > 0) {
			flags |= CORNER_MASKS[3];
		}
		if (functionValues[x][y][z + 1] > 0) {
			flags |= CORNER_MASKS[4];
		}
		if (functionValues[x + 1][y][z + 1] > 0) {
			flags |= CORNER_MASKS[5];
		}
		if (functionValues[x][y + 1][z + 1] > 0) {
			flags |= CORNER_MASKS[6];
		}
		if (functionValues[x + 1][y + 1][z + 1] > 0) {
			flags |= CORNER_MASKS[7];
		}
		return flags;
	}

	/**
	 * Computes the potential gradient at (x,y,z).
	 */
	private float[] computeGradientDirection(float x, float y, float z) {
		final float vertexValue = function.evaluate(x, y, z);
		final float xDerivative = computePartialDerivative(x, y, z, 0,
				vertexValue);
		final float yDerivative = computePartialDerivative(x, y, z, 1,
				vertexValue);
		final float zDerivative = computePartialDerivative(x, y, z, 2,
				vertexValue);

		// normalize
		final float amplitude = (float) Math.sqrt(xDerivative * xDerivative
				+ yDerivative * yDerivative + zDerivative * zDerivative);

		return new float[] { xDerivative / amplitude, yDerivative / amplitude,
				zDerivative / amplitude };
	}

	/**
	 * Computes a partial derivative of the function.
	 * 
	 * @param direction
	 *            0/1/2 for derivative in x/y/z direction respectively.
	 * @param vertexValue
	 *            Pre-computed function value at (x,y,z).
	 */
	private float computePartialDerivative(float x, float y, float z,
			int direction, float vertexValue) {
		if (direction == 0) {
			x += DERIVATIVE_DELTA_CONSTANT;
		} else if (direction == 1) {
			y += DERIVATIVE_DELTA_CONSTANT;
		} else if (direction == 2) {
			z += DERIVATIVE_DELTA_CONSTANT;
		}
		final float deltaValue = function.evaluate(x, y, z);
		return (deltaValue - vertexValue) / DERIVATIVE_DELTA_CONSTANT;
	}

	private float computeLinearIntercept(float loVal, float hiVal) {
		float delta = loVal - hiVal;
		float intercept = loVal / delta;
		return intercept;
	}
}
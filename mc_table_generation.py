#!/usr/bin/env python

# Script that generates triangle specifications for all
# possible combinations of corner signs for the marching
# cubes algorithm.
# 
# Assumes that the corner signs are encoded as integers
# where bit i indicates whether corner i (as given by the
# corner index specification below) is positive or not.
#
# Outputs two Java arrays code corresponding to:
#   - The number of triangles for each sign combination.
#   - The edges of the vertices for these triangles (as
#     given by the edge index specification below).
#
# Operates by taking a minimal set of pre-specified
# triangles and applying three operations (two rotations
# and an inversion) to them until all combinations have
# been generated.
#
# Corner indices:
#      6--------------7
#     /:             /|  
#    / :            / |  
#   /  :           /  |  
#  4--------------5   |  
#  |   :          |   |  
#  |   :          |   |  
#  |   2 - - - - -|- -3
#  |  /           |  /
#  | /            | /
#  |/             |/
#  0--------------1
#
# Edge indices:
#      -------10-------
#     /:             /|  
#   11 :            9 |  
#   /  6           /  5  
#  -------8--------   |  
#  |   :          |   |  
#  |   :          |   |  
#  |   - - - -2- -|- -|
#  7  /           4  /
#  | 3            | 1
#  |/             |/
#  -------0--------

import itertools
import pprint
import ipdb


# Which corners indices map to which under the operations
X_ROTATION_CORNER_MAP = [1, 5, 3, 7, 0, 4, 2, 6]
Y_ROTATION_CORNER_MAP = [4, 5, 0, 1, 6, 7, 2, 3]
Z_ROTATION_CORNER_MAP = [2, 0, 3, 1, 6, 4, 7, 5]
INVERSION_CORNER_MAP = [1, 0, 3, 2, 5, 4, 7, 6]

# Which edges indices map to which under the operations
X_ROTATION_EDGE_MAP = [7, 3, 6, 11, 0, 2, 10, 8, 4, 1, 5, 9]
Y_ROTATION_EDGE_MAP = [2, 5, 10, 6, 1, 9, 11, 3, 0, 4, 8, 7]
Z_ROTATION_EDGE_MAP = [1, 2, 3, 0, 5, 6, 7, 4, 9, 10, 11, 8]
INVERSION_EDGE_MAP = [0, 3, 2, 1, 7, 6, 5, 4, 8, 11, 10, 9]

MAX_PERMUTATIONS = 8

# Initial triangle specification, given as a dict {a: (b, c), ...} where:
#   - a is an integer containing the corner flags for the cube.
#   - b is the number of corresponding triangles
#   - c is a list of the edge indices of the corresponding triangles
INITIAL_SPECIFICATIONS = {0b00000000: (0, []),
                          0b00000001: (1, [0,3,7]),
                          0b00000011: (2, [4,1,7,7,1,3]),
                          0b00100001: (2, [0,3,7,8,9,4]),
                          0b00001110: (3, [0,4,3,3,4,6,6,4,5]),
                          0b00001111: (2, [4,5,7,7,5,6]),
                          0b00011110: (4, [0,4,3,3,4,6,6,4,5,11,8,7]),
                          0b01101001: (4, [0,3,7,1,5,2,8,9,4,10,11,6]),
                          0b01001101: (4, [1,5,0,0,5,10,0,10,7,7,10,11]),
                          0b01001110: (4, [4,5,0,0,5,11,10,11,5,11,3,0]),
                          0b10000001: (2, [0,3,7,5,9,10]),
                          0b10000011: (3, [4,1,7,7,1,3,9,10,5]),
                          0b10010010: (3, [4,1,0,11,8,7,9,10,5]),
                          0b10011001: (4, [8,0,11,11,0,3,9,10,1,1,10,2]),
                          0b10001101: (4, [1,9,0,0,9,6,6,9,10,0,6,7])}

def apply_maps(corner_flags, edges, corner_map, edge_map):
    new_corner_flags = 0
    for i in range(8):
        new_corner_flags += ((corner_flags >> corner_map[i]) & 1) << i

    new_triangles = [edge_map[edge] for edge in edges]
    return new_corner_flags, new_triangles


# Takes lists of corner flags and edge indices and returns two lists
# containing the elements they are mapped to under a rotation about the
# x axis.
def x_rotation(corner_flags, edges): 
    return apply_maps(corner_flags, edges, X_ROTATION_CORNER_MAP, X_ROTATION_EDGE_MAP)


def y_rotation(corner_flags, edges): 
    return apply_maps(corner_flags, edges, Y_ROTATION_CORNER_MAP, Y_ROTATION_EDGE_MAP)


def z_rotation(corner_flags, edges): 
    return apply_maps(corner_flags, edges, Z_ROTATION_CORNER_MAP, Z_ROTATION_EDGE_MAP)


# Takes lists of corner flags and edge indices and returns two lists
# containing the elements they are mapped to under the inversion. 
def inversion(corner_flags, edges): 
    new_corner_flags, new_edges = apply_maps(corner_flags, edges, INVERSION_CORNER_MAP, INVERSION_EDGE_MAP)
    new_edges = flip_normals(new_edges)
    return new_corner_flags, new_edges


def sign_flip(corner_flags, edges):
    return corner_flags ^ 0b11111111, flip_normals(edges)


def flip_normals(edges):
    if len(edges) == 0:
        return edges
    for i in range(0, len(edges), 3):
        temp = edges[i]
        edges[i] = edges[i + 1]
        edges[i + 1] = temp
    return edges


def print_specifications(specifications):
    triangle_counts = []
    edges = []
    for i in range(256):
        triangle_counts.append(specifications[i][0] if specifications.has_key(i) else 0)
        edges.append(specifications[i][1] if specifications.has_key(i) else [])
    print "final static int[] TRIANGLE_COUNTS = %s;" % str(triangle_counts).replace('[','{').replace(']','}')
    print "final static int[][] TRIANGLE_SPECIFICATIONS = %s;" % str(edges).replace('[','{').replace(']','}')


specifications = INITIAL_SPECIFICATIONS.copy()
available_operations = [x_rotation, z_rotation, y_rotation, sign_flip]

# generate all combinations by brute force
for permutation in itertools.combinations_with_replacement(available_operations, MAX_PERMUTATIONS):
    for corner_flags, (triangle_count, edges) in INITIAL_SPECIFICATIONS.iteritems():
        for operation in permutation:
            corner_flags, edges = operation(corner_flags, edges)
            specifications[corner_flags] = (triangle_count, edges) 

print_specifications(specifications)

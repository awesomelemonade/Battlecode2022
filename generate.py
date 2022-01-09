import math
from itertools import product

# Generated9
squareLength = 7
scanRadiusSquared = 9
edgeThreshold = 4
useIsLocationOccupied = False
# Generated13
# squareLength = 7
# scanRadiusSquared = 13
# edgeThreshold = 8
# useIsLocationOccupied = False
# Generated34
# squareLength = 11
# scanRadiusSquared = 34
# edgeThreshold = 25
# useIsLocationOccupied = True # For archons

ourLocationVar = "ourLocation"
ourLocationXVar = "ourLocationX"
ourLocationYVar = "ourLocationY"

def genVars(prefix):
    return [[prefix + str(x) + "_" + str(y) for x in range(squareLength)] for y in range(squareLength)]


dpVariables = genVars("dp_")
dirVariables = genVars("dir_")
rubbleVariables = genVars("rubble_")
locationVariables = genVars("loc_")
scoreVariables = genVars("score_")
nextVariables = genVars("next_")
offsetX = int(squareLength / 2)
offsetY = int(squareLength / 2)

locationVariables[offsetX][offsetY] = ourLocationVar # Special Case

allVisionCoords = [(x, y) for x, y in product(range(squareLength), range(squareLength)) if (x - offsetX) ** 2 + (y - offsetY) ** 2 <= scanRadiusSquared]
allVisionCoords = sorted(allVisionCoords, key=lambda coord: math.atan2(coord[1] - offsetY, coord[0] - offsetX))

# Define variables

for x, y in allVisionCoords:
    dpVar = dpVariables[x][y]
    dirVar = dirVariables[x][y]
    rubbleVar = rubbleVariables[x][y]
    locationVar = locationVariables[x][y]
    scoreVar = scoreVariables[x][y]
    nextVar = nextVariables[x][y]
    if not (x == offsetX and y == offsetY):
        print("public static double {};".format(dpVar))
    print("public static Direction {};".format(dirVar))
    print("public static double {};".format(rubbleVar))
    print("public static MapLocation {};".format(locationVar))
    print("public static double {};".format(scoreVar))
    print("public static double {};".format(nextVar))
print("public static Direction bestDir;")
print("public static double bestScore;")
print("public static int {};".format(ourLocationXVar))
print("public static int {};".format(ourLocationYVar))
print("public static MapLocation target;")

# Define method
print("public static Direction execute(MapLocation t) throws GameActionException {")
# Initial setup
print("{} = rc.getLocation();".format(ourLocationVar))
print("if({}.equals(t)) {{".format(ourLocationVar))
print("return Direction.CENTER;")
print("}")
print("target = t;")
print("{} = {}.x;".format(ourLocationXVar, ourLocationVar))
print("{} = {}.y;".format(ourLocationYVar, ourLocationVar))

def generateBounded(minX, maxX, minY, maxY):
    # Initialize Location Variables
    print ("// START BOUNDED: minX={}, maxX={}, minY={}, maxY={}".format(minX, maxX, minY, maxY))

    visionCoords = [(x, y) for x, y in allVisionCoords if x >= minX and x <= maxX and y >= minY and y <= maxY]
    # print("// " + str(visionCoords) + str(allVisionCoords))
    sortedForInitializingLocations = sorted(visionCoords, key=lambda coord: (abs(coord[0] - offsetX), abs(coord[1] - offsetY)))

    adjacentCoords = [(1, 0), (0, 1), (-1, 0), (0, -1), (1, 1), (-1, 1), (-1, -1), (1, -1)]
    adjacentDirections = ["EAST", "NORTH", "WEST", "SOUTH", "NORTHEAST", "NORTHWEST", "SOUTHWEST", "SOUTHEAST"]
    adjacentDirectionsMapping = dict(zip(adjacentCoords, adjacentDirections))

    for x, y in sortedForInitializingLocations:
        dx, dy = x - offsetX, y - offsetY
        if dx == 0 and dy == 0:
            continue
        locationVar = locationVariables[x][y]
        if (dx, dy) in adjacentDirectionsMapping:
            direction = adjacentDirectionsMapping[(dx, dy)]
            print("{} = rc.adjacentLocation(Direction.{});".format(locationVar, direction))
        else:
            if dx == 0:
                if dy > 0:
                    print("{} = {}.add(Direction.NORTH);".format(locationVar, locationVariables[x][y - 1]))
                else:
                    print("{} = {}.add(Direction.SOUTH);".format(locationVar, locationVariables[x][y + 1]))
            elif dx < 0:
                print("{} = {}.add(Direction.WEST);".format(locationVar, locationVariables[x + 1][y]))
            else:
                print("{} = {}.add(Direction.EAST);".format(locationVar, locationVariables[x - 1][y]))

    # Initialize dp variables
    for x, y in visionCoords:
        dx, dy = x - offsetX, y - offsetY
        dpVar = dpVariables[x][y]
        rubbleVar = rubbleVariables[x][y]
        locationVar = locationVariables[x][y]
        if dx == 0 and dy == 0:
            pass
        else:
            print("{} = Double.MAX_VALUE;".format(dpVar))
        print("{} = 1.0 + rc.senseRubble({}) / 10.0;".format(rubbleVar, locationVar))

    # Populate adjacent squares
    for (dx, dy), direction in adjacentDirectionsMapping.items():
        x, y = offsetX + dx, offsetY + dy
        if x < minX or x > maxX or y < minY or y > maxY:
            continue
        locationVar = locationVariables[x][y]
        dirVar = dirVariables[x][y]
        dpVar = dpVariables[x][y]
        if useIsLocationOccupied:
            print("if (!rc.isLocationOccupied({})) {{".format(locationVar))
        else:
            print("if (rc.canMove(Direction.{})) {{".format(direction))
        print("{} = {};".format(dpVar, rubbleVariables[offsetX][offsetY]))
        print("{} = Direction.{};".format(dirVar, direction))
        print("}")
        

    # Populate the rest of the squares
    potentiallyNotInfinityVars = set()

    for dist in range(1, scanRadiusSquared + 1):
        for x, y in visionCoords:
            dx, dy = x - offsetX, y - offsetY
            if dx * dx + dy * dy == dist:
                # calculate potential dp of next square
                nextVar = nextVariables[x][y]
                definedNextVar = False
                
                # dp transition
                for adx, ady in adjacentCoords:
                    dx2, dy2 = dx + adx, dy + ady
                    dist2 = dx2 * dx2 + dy2 * dy2
                    x2, y2 = dx2 + offsetX, dy2 + offsetY
                    if x2 < minX or x2 > maxX or y2 < minY or y2 > maxY:
                        continue
                    if dist2 <= scanRadiusSquared and dist2 > dist and dist2 > 2:
                        if not definedNextVar:
                            print("{} = {} + {};".format(nextVar, dpVariables[x][y], rubbleVariables[x][y])) # Could potentially move into static variables
                            definedNextVar = True
                        dpNextVar = dpVariables[x2][y2]
                        if dpNextVar in potentiallyNotInfinityVars:
                            print("if ({} < {}) {{".format(nextVar, dpNextVar))
                        print("{} = {};".format(dpNextVar, nextVar))
                        print("{} = {};".format(dirVariables[x2][y2], dirVariables[x][y]))
                        if dpNextVar in potentiallyNotInfinityVars:
                            print("}")
                        potentiallyNotInfinityVars.add(dpNextVar)

    '''
    # Print out dp
    for i in range(squareLength):
        formatString = ', '.join(['%.02f'] * squareLength)
        print('System.out.printf("{}\\n", '.format(formatString), end='')
        printedVars = ['0f' for j in range(squareLength)]
        for j in range(squareLength):
            if (i - offsetX) ** 2 + (j - offsetY) ** 2 <= scanRadiusSquared:
                if not (i == offsetX and j == offsetY):
                    printedVars[j] = "{} == Double.MAX_VALUE ? -1f : {}".format(dpVariables[i][j], dpVariables[i][j])
        print(', '.join(printedVars), end='')
        print(');')
    '''

    # Retrieve best direction
    print("switch (target.x - {}) {{".format(ourLocationXVar))
    for x in range(squareLength):
        if x < minX or x > maxX:
            continue
        dx = x - offsetX
        print("case {}:".format(dx))
        print("switch (target.y - {}) {{".format(ourLocationYVar))
        for y in range(squareLength):
            if y < minY or y > maxY:
                continue
            dy = y - offsetY
            if dx * dx + dy * dy <= scanRadiusSquared:
                print("case {}:".format(dy))
                print("return {};".format(dirVariables[x][y]))
        print("}")
        print("break;")
    print("}")
    print("bestDir = null;")
    print("bestScore = Double.MAX_VALUE;")
    for x, y in visionCoords:
        dx, dy = x - offsetX, y - offsetY
        if dx * dx + dy * dy >= edgeThreshold:
            dpVar = dpVariables[x][y]
            rubbleVar = rubbleVariables[x][y]
            locationVar = locationVariables[x][y]
            dirVar = dirVariables[x][y]
            scoreVar = scoreVariables[x][y]
            print("{} = {} + {} + Math.sqrt({}.distanceSquaredTo(target)) * 8.0;".format(scoreVar, dpVar, rubbleVar, locationVar))
            print("if ({} < bestScore) {{".format(scoreVar))
            print("bestScore = {};".format(scoreVar))
            print("bestDir = {};".format(dirVar))
            print("}")
    print("return bestDir;")
    print ("// END BOUNDED: minX={}, maxX={}, minY={}, maxY={}".format(minX, maxX, minY, maxY))

# switch on ourLocation.x, ourLocation.y, Constants.MAP_WIDTH - ourLocation.x, Constants.MAP_HEIGHT - ourLocation.y
boundedFunctions = set()

print("switch ({}) {{".format(ourLocationXVar))
# case 0 to offsetX - 1
for i in range(offsetX):
    minX = offsetX - i
    maxX = squareLength - 1
    print("case {}:".format(i))
    # switch on y
    print("switch ({}) {{".format(ourLocationYVar))
    # case 0 to offsetY - 1
    for j in range(offsetY):
        minY = offsetY - j
        maxY = squareLength - 1
        print("case {}:".format(j))
        print("return executeBounded_{}_{}_{}_{}();".format(minX, maxX, minY, maxY))
        boundedFunctions.add((minX, maxX, minY, maxY))
    print("}")
    print("switch (Constants.MAP_HEIGHT - {}) {{".format(ourLocationYVar))
    for j in range(1, offsetY + 1):
        minY = 0
        maxY = offsetY + j - 1
        print("case {}:".format(j))
        print("return executeBounded_{}_{}_{}_{}();".format(minX, maxX, minY, maxY))
        boundedFunctions.add((minX, maxX, minY, maxY))
    print("}")
    minY, maxY = 0, squareLength - 1
    print("return executeBounded_{}_{}_{}_{}();".format(minX, maxX, minY, maxY))
    boundedFunctions.add((minX, maxX, minY, maxY))
print("}")

print("switch (Constants.MAP_WIDTH - {}) {{".format(ourLocationXVar))
# case 1 to offsetX
for i in range(1, offsetX + 1):
    minX = 0
    maxX = offsetX + i - 1
    print("case {}:".format(i))
    # switch on y
    print("switch ({}) {{".format(ourLocationYVar))
    # case 0 to offsetY - 1
    for j in range(offsetY):
        minY = offsetY - j
        maxY = squareLength - 1
        print("case {}:".format(j))
        print("return executeBounded_{}_{}_{}_{}();".format(minX, maxX, minY, maxY))
        boundedFunctions.add((minX, maxX, minY, maxY))
    print("}")
    print("switch (Constants.MAP_HEIGHT - {}) {{".format(ourLocationYVar))
    for j in range(1, offsetY + 1):
        minY = 0
        maxY = offsetY + j - 1
        print("case {}:".format(j))
        print("return executeBounded_{}_{}_{}_{}();".format(minX, maxX, minY, maxY))
        boundedFunctions.add((minX, maxX, minY, maxY))
    print("}")
    minY, maxY = 0, squareLength - 1
    print("return executeBounded_{}_{}_{}_{}();".format(minX, maxX, minY, maxY))
    boundedFunctions.add((minX, maxX, minY, maxY))
print("}")

# test for y only
minX, maxX = 0, squareLength - 1
# switch on y
print("switch ({}) {{".format(ourLocationYVar))
# case 0 to offsetY - 1
for j in range(offsetY):
    minY = offsetY - j
    maxY = squareLength - 1
    print("case {}:".format(j))
    print("return executeBounded_{}_{}_{}_{}();".format(minX, maxX, minY, maxY))
    boundedFunctions.add((minX, maxX, minY, maxY))
print("}")
print("switch (Constants.MAP_HEIGHT - {}) {{".format(ourLocationYVar))
for j in range(1, offsetY + 1):
    minY = 0
    maxY = offsetY + j - 1
    print("case {}:".format(j))
    print("return executeBounded_{}_{}_{}_{}();".format(minX, maxX, minY, maxY))
    boundedFunctions.add((minX, maxX, minY, maxY))
print("}")
minY, maxY = 0, squareLength - 1
print("return executeBounded_{}_{}_{}_{}();".format(minX, maxX, minY, maxY))
boundedFunctions.add((minX, maxX, minY, maxY))

# End method
print("}")

for a, b, c, d in boundedFunctions:
    print("public static Direction executeBounded_{}_{}_{}_{}() throws GameActionException {{".format(a, b, c, d))
    generateBounded(a, b, c, d)
    print("}")
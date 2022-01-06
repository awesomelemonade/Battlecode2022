import math
from itertools import product

squareLength = 9
scanRadiusSquared = 20

ourLocationVar = "ourLocation"

def genVars(prefix):
    return [[prefix + str(x) + "_" + str(y) for x in range(squareLength)] for y in range(squareLength)]

dpVariables = genVars("dp_")
dirVariables = genVars("dir_")
rubbleVariables = genVars("rubble_")
onTheMapVariables = genVars("onTheMap_")
locationVariables = genVars("loc_")
offsetX = int(squareLength / 2)
offsetY = int(squareLength / 2)

visionCoords = [(x, y) for x, y in product(range(squareLength), range(squareLength)) if (x - offsetX) ** 2 + (y - offsetY) ** 2 <= scanRadiusSquared]
visionCoords = sorted(visionCoords, key=lambda coord: math.atan2(coord[1] - offsetY, coord[0] - offsetX))

# Define variables

for x, y in visionCoords:
    dpVar = dpVariables[x][y]
    dirVar = dirVariables[x][y]
    rubbleVar = rubbleVariables[x][y]
    onTheMapVar = onTheMapVariables[x][y]
    locationVar = locationVariables[x][y]
    print("public static double {};".format(dpVar))
    print("public static Direction {};".format(dirVar))
    print("public static double {};".format(rubbleVar))
    print("public static boolean {};".format(onTheMapVar))
    print("public static MapLocation {};".format(locationVar))
print("public static MapLocation {};".format(ourLocationVar))


# Define method
print("public static Direction execute(MapLocation target) throws GameActionException {")
print("{} = rc.getLocation();".format(ourLocationVar))
print("""
if (rc.getLocation().equals(target)) {
    return Direction.CENTER;
}
""")
# Initialize dp variables
for x, y in visionCoords:
    dx, dy = x - offsetX, y - offsetY
    dpVar = dpVariables[x][y]
    rubbleVar = rubbleVariables[x][y]
    onTheMapVar = onTheMapVariables[x][y]
    locationVar = locationVariables[x][y]
    if dx == 0 and dy == 0:
        print("{} = 0;".format(dpVar))
    else:
        print("{} = Double.MAX_VALUE;".format(dpVar))
    print("{} = ourLocation.translate({}, {});".format(locationVar, dx, dy))
    print("{} = rc.onTheMap({});".format(onTheMapVar, locationVar))
    print("if ({}) {{".format(onTheMapVar))
    print("{} = 1.0 + rc.senseRubble({}) / 10.0;".format(rubbleVar, locationVar))
    print("}")

# Populate adjacent squares
adjacentCoords = [(1, 0), (0, 1), (-1, 0), (0, -1), (1, 1), (-1, 1), (-1, -1), (1, -1)]
adjacentDirections = ["EAST", "NORTH", "WEST", "SOUTH", "NORTHEAST", "NORTHWEST", "SOUTHWEST", "SOUTHEAST"]
for direction, (dx, dy) in zip(adjacentDirections, adjacentCoords):
    x, y = offsetX + dx, offsetY + dy
    onTheMapVar = onTheMapVariables[x][y]
    locationVar = locationVariables[x][y]
    dirVar = dirVariables[x][y]
    print("if ({} && !rc.isLocationOccupied({})) {{".format(onTheMapVar, locationVar))
    print("{} = {};".format(dpVar, rubbleVariables[offsetX][offsetY]))
    print("{} = Direction.{};".format(dirVar, direction))
    print("}")
    

# Populate the rest of the squares
for dist in range(1, scanRadiusSquared + 1):
    for x, y in visionCoords:
        dx, dy = x - offsetX, y - offsetY
        if dx * dx + dy * dy == dist:
            # calculate potential dp of next square
            nextVar = "next_{}_{}".format(x, y)
            definedNextVar = False
            
            # dp transition
            for adx, ady in adjacentCoords:
                dx2, dy2 = dx + adx, dy + ady
                dist2 = dx2 * dx2 + dy2 * dy2
                x2, y2 = dx2 + offsetX, dy2 + offsetY
                if dist2 <= scanRadiusSquared and dist2 > dist:
                    if not definedNextVar:
                        print("double {} = {} + {};".format(nextVar, dpVariables[x][y], rubbleVariables[x][y])) # Could potentially move into static variables
                        definedNextVar = True
                    print("if ({}) {{".format(onTheMapVariables[x2][y2]))
                    print("if ({} < {}) {{".format(nextVar, dpVariables[x2][y2]))
                    print("{} = {};".format(dpVariables[x2][y2], nextVar))
                    print("{} = {};".format(dirVariables[x2][y2], dirVariables[x][y]))
                    print("}")
                    print("}")

# Retrieve best direction
print("if ({}.isWithinDistanceSquared(target, {})) {{".format(ourLocationVar, scanRadiusSquared))
print("switch (target.x - {}.x) {{".format(ourLocationVar))
for x in range(squareLength):
    dx = x - offsetX
    print("case {}:".format(dx))
    print("switch (target.y - {}.y) {{".format(ourLocationVar))
    for y in range(squareLength):
        dy = y - offsetY
        if dx * dx + dy * dy <= scanRadiusSquared:
            print("case {}:".format(dy))
            print("return {};".format(dirVariables[x][y]))
    print("}")
    print("break;")
print("}")
print("return null;")
print("} else {")
print("Direction bestDir = null;")
print("double bestScore = Double.MAX_VALUE;")
for x, y in visionCoords:
    dx, dy = x - offsetX, y - offsetY
    if dx * dx + dy * dy >= 13:
        dpVar = dpVariables[x][y]
        onTheMapVar = onTheMapVariables[x][y]
        rubbleVar = rubbleVariables[x][y]
        locationVar = locationVariables[x][y]
        dirVar = dirVariables[x][y]
        print("if ({}) {{".format(onTheMapVar))
        print("double score = {} + {} + (Math.sqrt({}.distanceSquaredTo(target)) - 1.0) * 8.0;".format(dpVar, rubbleVar, locationVar))
        print("if (score < bestScore) {")
        print("bestScore = score;")
        print("bestDir = {};".format(dirVar))
        print("}")
        print("}")
print("return bestDir;")
print("}")

# End method
print("}")

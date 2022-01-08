import subprocess

emojiMode = True
emojiMap = {
    'Won': ':heavy_check_mark:',
    'Lost': ':x:',
    'Tied': ':warning:',
    'N/A': ':heavy_minus_sign:',
    'Error': ':heavy_exclamation_mark:'
}
errors = []
currentBot = 'bot'
matches = {('examplefuncsplayer', 'eckleburg'), ('smartie', 'maptestsmall')}

bots = ['examplefuncsplayer', 'smartie']
botsSet = set(bots)
maps = ['maptestsmall', 'eckleburg', 'intersection']
mapsSet = set(maps)

numWinsMapping = {
    0: 'Lost',
    1: 'Tied',
    2: 'Won',
}
def run_match(bot, map):
    print("Running {} vs {} on {}".format(currentBot, bot, map))
    try:
        #outputA = str(subprocess.check_output(['ls'], shell=True))
        #outputA = str(subprocess.check_output(['./gradlew', 'run', '-PteamA=' + currentBot, '-PteamB=' + bot, '-Pmaps="' + map + '"'], shell=True))
        #outputB = str(subprocess.check_output(['./gradlew', 'run', '-PteamA=' + bot, '-PteamB=' + currentBot, '-Pmaps="' + map + '"'], shell=True))
        resultA = subprocess.run(['./gradlew', 'tasks'], stdout=subprocess.PIPE)
        outputA = str(resultA.stdout)
        print(outputA)
        outputB = ''
    except subprocess.CalledProcessError as exc:
        print("Status: FAIL", exc.returncode, exc.output)
        return 'Error'
    else:
        print(outputA)
        print(outputB)
        winAString = '{} (A) wins'.format(currentBot)
        winBString = '{} (B) wins'.format(currentBot)
        numWins = 0
        if winAString in outputA:
            numWins += 1
        if winBString in outputB:
            numWins += 1
        return numWinsMapping[numWins]


results = {}
# Run matches
for bot, map in matches:
    # Verify match is valid
    if not bot in botsSet or not map in mapsSet:
        errors.append('Unable to parse bot={}, map={}'.format(bot, map))
    # run run_match.py
    
    results[(bot, map)] = run_match(bot, map)

# Construct table
table = [[results.get((bot, map), 'N/A') for bot in bots] for map in maps]

if emojiMode:
    table = [[emojiMap.get(item, item) for item in row] for row in table]

# Write to file
with open('matches-summary.txt', 'w') as f:
    table = [[''] + bots, [':---:' for i in range(len(bots) + 1)]] + [[map] + row for map, row in zip(maps, table)]
    for line in table:
        f.write('| ')
        f.write(' | '.join(line))
        f.write(' |')
        f.write('\n')
    f.write('\n')
    for error in errors:
        f.write(error)
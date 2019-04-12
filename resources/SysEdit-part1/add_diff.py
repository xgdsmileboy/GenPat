import os
import sys
import json

for i in range(1, 50):
    if os.path.exist('./' + str(i)):
        t = json.load('./%d/info.json' % i)
        with open('./%d/all.diff' % t[0][1]

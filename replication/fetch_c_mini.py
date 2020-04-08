import json
import requests
import os
import codecs
import xml.etree.ElementTree as ET  
import sys
from multiprocessing import Pool

repo_sname = sys.argv[1]
repo_gitname = sys.argv[2]

print('repo_sname=', repo_sname)
print('repo_gitname=', repo_gitname)

# cluster_save_path = './cluster_onlyfirst2'
cluster_save_path = './c3_only_f2'

file = '/home/renly/cthree/data/%s.json' % repo_sname

with codecs.open(file, 'r', 'utf-8') as f:
    d = json.load(f)

print("total clusters=", len(d['clusters']))


select = set()

'''
xml= './mini/%s_all-mini.xml' % repo_sname
tree = ET.parse(xml) 
root = tree.getroot()

for ele in root:
    v = ele[2].text
    r, c, p0, p1 = v.split(',')
    select.add((int(c), int(p0)))
    select.add((int(c), int(p1)))
'''

a = {}
c_num = 0

for c in d['clusters']:
    c_num += 1
    c['num'] = c_num
    
def work(c):
    c_num = c['num']
    c_path = cluster_save_path + '/%s_cluster/%d' % (repo_sname, c_num)
    
#     if os.path.exists(c_path):
#         continue
    
    def safe_write(file, obj):
        path = os.path.dirname(file)
        if not os.path.exists(path):
            os.makedirs(path)
        with codecs.open(file, "w", "utf-8") as write_file:
            write_file.write(obj)
        print('finish write %s to file....' % file)

    safe_write(c_path + '/info.json', json.dumps(c))
    
    p_num = -1
    info = []
    for p in c['members']:
        p_num += 1
        
        if p_num > 1:
            break

        '''
        if not ((p_num <= 1) or ((c_num, p_num) in select)):
            continue
        '''
        
        r = repo_gitname

        def try_fetch(r, p):
            try:
                src = requests.get('https://github.com/%s/raw/%s/%s' % (r, p['commitBeforeChange'], p['fileName']))
                tar = requests.get('https://github.com/%s/raw/%s/%s' % (r, p['commitAfterChange'], p['fileName']))

                safe_write('%s/src_%d.java' % (c_path, p_num), src.text)
                safe_write('%s/tar_%d.java' % (c_path, p_num), tar.text)
                return True
            except:
                return False
        
        ok = False
        for i in range(5):
            if try_fetch(r, p):
                ok = True
                break
        if not ok:
            with open(repo_sname + '_log.txt', 'a') as f:
                f.write('error on %s %d\n' % (c_path, p_num))


with Pool(processes=60) as pool:
    result = pool.map(work, d['clusters'])

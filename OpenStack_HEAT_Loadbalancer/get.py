#!/usr/bin/env python3
"""
    Script that does n asynchronous request to a url. 
    Outputs the total and mean time of the execution

"""
from  concurrent.futures import ThreadPoolExecutor, as_completed
import urllib.request

import time
import sys
import os


cpu = 3                     # <---- NUMBER OF THREAD
n = 50                      # <---- NUMBER OF REQUEST

def func_get(url):
    start = time.time()
    with urllib.request.urlopen(url) as conn:
        conn.read()
    end = time.time()
    return end - start

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print ("usage: get.py <url>")
        sys.exit(1)

    url = sys.argv[1]
    aggregate_time  = 0

    print ("%d request for %s" % (n,url))
    with ThreadPoolExecutor(max_workers=cpu) as executor:
        submit = {executor.submit(func_get, url): i for i in range(50)}
        for future in as_completed(submit):
            i = submit[future]
            try:
                data = future.result()
            except Exception as exc:
                print('%r generated an exception: %s' % (i, exc))
            else:
                print('%r executed in %f seconds' % (i, data))
                aggregate_time += data

    print("Total time: %f s" % (aggregate_time))
    print("Mean time: %f s" % (aggregate_time / n))


#!/usr/bin/python3
# -*- coding: utf-8 -*-

import sys
import os
import re
import jinja2
from collections import namedtuple

Location = namedtuple('Location', ['ts', 'lat', 'lon', 'accuracy', 'speed'])

def convertStrFloat(s):
	return s.replace(',', '.')

def getLocationList(path):
	locationList = []

	f = open(path)
	for line in f:
		#ts:1491495219000;lat:48,879167;long:2,359470;accuracy:197,000000;speed:0,000000
		pattern = re.compile("ts:(?P<ts>\d*?);lat:(?P<lat>\d*?,\d*?);long:(?P<long>\d*?,\d*?);accuracy:(?P<accuracy>\d*?,\d*?);speed:(?P<speed>\d*?,\d*?)")
		result = pattern.match(line)

		locationList.append(Location(
			ts=int(result.group("ts")),
			lat=float(convertStrFloat(result.group("lat"))),
			lon=float(convertStrFloat(result.group("long"))),
			accuracy=float(convertStrFloat(result.group("accuracy"))),
			speed=float(convertStrFloat(result.group("speed"))))
		)

	return locationList

def generateHtml(locationList, outPath):
	sourcePath = os.path.dirname(os.path.abspath(__file__))
	templateFile = open('%s/map.template' % sourcePath, 'r')
	template = jinja2.Template(templateFile.read())

	html = template.render(locationList=locationList)

	# Write output file
	out = open(outPath, 'w')
	out.write(html)

def convertFile(srcPath):
	outPath = srcPath.replace(".bin", ".html")

	print("%s => %s" % (srcPath, outPath))
	locationList = getLocationList(srcPath)
	if len(locationList) > 0:
		generateHtml(locationList, outPath)
	else:
		print("Empty file, skipped")

for path in sys.argv[1:]:
	convertFile(path)


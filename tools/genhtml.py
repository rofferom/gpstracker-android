#!/usr/bin/python3
# -*- coding: utf-8 -*-

import sys
import os
import re
import jinja2
from collections import namedtuple

# local packages
from location import locationFromStr

def getLocationList(path):
	locationList = []

	f = open(path)
	for line in f:
		location = locationFromStr(line)
		locationList.append(location)

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
	outPath = srcPath.replace(".txt", ".html")

	print("%s => %s" % (srcPath, outPath))
	locationList = getLocationList(srcPath)
	if len(locationList) > 0:
		generateHtml(locationList, outPath)
	else:
		print("Empty file, skipped")

for path in sys.argv[1:]:
	convertFile(path)


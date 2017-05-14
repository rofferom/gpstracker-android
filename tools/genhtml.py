#!/usr/bin/python3
# -*- coding: utf-8 -*-

import sys
import os
import re
import argparse
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
	templateFile = open(os.path.join(sourcePath, 'map.template'), 'r')
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

def parseArgs():
	parser = argparse.ArgumentParser(description='Generate html files from GpsTracker record files.')

	parser.add_argument("-f", "--files", nargs="+", help="List of files to parse")

	return (parser, parser.parse_args())

if __name__ == '__main__':
	(argParser, args) = parseArgs()

	# Parse a file list
	if args.files:
		for path in args.files:
			convertFile(path)
	else:
		argParser.print_help()


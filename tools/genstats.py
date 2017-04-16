#!/usr/bin/python3
# -*- coding: utf-8 -*-

import sys
import re
import os
import statistics
import jinja2
from collections import namedtuple

# local packages
from location import locationFromStr

Record = namedtuple('Record', ['handler', 'hasArgs'])

def parseAndHandleRecord(ts, recordStr, recordDescList):
	pattern = re.compile("(?P<name>[A-Za-z]*);?")
	result = pattern.match(recordStr)

	name = result.group("name")

	recordDesc = recordDescList[name]
	if recordDesc.hasArgs:
		# Skip trailing ';'
		endIdx = result.end(result.lastgroup) + 1
		recordArgs = recordStr[endIdx:]

		recordDesc.handler(ts, recordArgs)
	else:
		recordDesc.handler(ts)

BatteryLevel = namedtuple('Location', ['ts', 'level'])

class BatteryHandler:
	def __init__(self):
		self.firstRecord = None
		self.lastRecord = None

	def handleRecord(self, ts, record):
		value = float(record)

		if not self.firstRecord:
			self.firstRecord = BatteryLevel(ts, value)

		self.lastRecord = BatteryLevel(ts, value)

	def displayResult(self):
		print("Battery")
		print("\tWent from %.02f%% to %.02f%%" % (self.firstRecord.level, self.lastRecord.level))

		lost = self.firstRecord.level - self.lastRecord.level
		strDuration = msToStrHours(self.lastRecord.ts - self.firstRecord.ts)

		print("\tLost: %.02f%% in %s" % (lost, strDuration))

class GpsHandlerCb:
	def startAcq(self, ts):
		pass

	def timeout(self, ts):
		pass

	def validPoint(self, ts):
		pass

	def newLocation(self, ts, location):
		pass

class GpsAccuracyStats(GpsHandlerCb):
	ChartAccuracy = namedtuple('ChartLocation', ['ts', 'accuracy'])

	def __init__(self):
		super().__init__()

		self.recordList = []
		self.firstTs = None
		self.lastTs = None

	def rescaleTs(self, ts):
		if not self.firstTs:
			self.firstTs = ts

		# ms => s
		return int((ts - self.firstTs) / 1000)

	def addMissingValues(self, ts):
		if not self.lastTs:
			return

		missingTs = self.lastTs + 1
		while missingTs < ts:
			chartAccuracy = GpsAccuracyStats.ChartAccuracy(ts=missingTs, accuracy=0)
			self.recordList.append(chartAccuracy)

			missingTs += 1

	def startAcq(self, ts):
		ts = self.rescaleTs(ts)
		self.addMissingValues(ts)

		chartAccuracy = GpsAccuracyStats.ChartAccuracy(ts=ts, accuracy=0)
		self.recordList.append(chartAccuracy)

		self.lastTs = ts

	def newLocation(self, ts, location):
		ts = self.rescaleTs(ts)
		self.addMissingValues(ts)

		chartAccuracy = GpsAccuracyStats.ChartAccuracy(ts=ts, accuracy=location.accuracy)

		self.recordList.append(chartAccuracy)
		self.lastTs = ts

	def genChart(self, outPath):
		sourcePath = os.path.dirname(os.path.abspath(__file__))
		templateFile = open('%s/average.template' % sourcePath, 'r')
		template = jinja2.Template(templateFile.read())

		html = template.render(accuracyList=self.recordList)

		# Write output file
		out = open(outPath, 'w')
		out.write(html)


class AppHandler:
	def __init__(self):
		self.gpsAccuracy = 0.0
		self.gpsAcqPeriod = 0
		self.gpsAcqTimeout = 0

		self.recordDescList = {
			"Config": Record(handler=self.handleConfig, hasArgs=True),
		}

	def handleRecord(self, ts, recordStr):
		parseAndHandleRecord(ts, recordStr, self.recordDescList)

	def handleConfig(self, ts, args):
		def convertStrFloat(s):
			return float(s.replace(',', '.'))

		# accuracy:10,0;gpsAcqPeriod:60;gpsAcqTimeout:60
		pattern = re.compile("gpsAccuracy:(?P<gpsAccuracy>\d*?,\d*?);gpsAcqPeriod:(?P<gpsAcqPeriod>\d*);gpsAcqTimeout:(?P<gpsAcqTimeout>\d*)")
		result = pattern.match(args)

		self.gpsAccuracy = float(result.group("gpsAccuracy").replace(',', '.'))
		self.gpsAcqPeriod = int(result.group("gpsAcqPeriod"))
		self.gpsAcqTimeout = int(result.group("gpsAcqTimeout"))

	def displayResult(self):
		print("Configuration")
		print("\tGps accuracy: %.02fm" % self.gpsAccuracy)
		print("\tGps acquisition period: %ds" % self.gpsAcqPeriod)
		print("\tGps acquisition timeout: %ds" % self.gpsAcqTimeout)

class GpsGeneralStats(GpsHandlerCb):
	def __init__(self):
		super().__init__()

		self.startAcqCount = 0
		self.validCount = 0
		self.timeoutCount = 0
		self.rawAccuracyList = []
		self.validAccuracyList = []

		self.lastStartAcqTs = None
		self.acqDurationList = []

		self.lastLocation = None
		self.timeoutAccuracyList = []

	def startAcq(self, ts):
		self.startAcqCount += 1
		self.lastStartAcqTs = ts

	def timeout(self, ts):
		self.timeoutCount += 1

		self.lastStartAcqTs = None

		if self.lastLocation:
			self.timeoutAccuracyList.append(self.lastLocation.accuracy)

		self.lastLocation = None

	def validPoint(self, ts):
		self.validCount += 1
		self.validAccuracyList.append(self.rawAccuracyList[-1])

		# ms => s
		duration = int((ts - self.lastStartAcqTs) / 1000)
		self.acqDurationList.append(duration)

		self.lastStartAcqTs = None
		self.lastLocation = None

	def newLocation(self, ts, location):
		self.rawAccuracyList.append(location.accuracy)
		self.lastLocation = location

	def displayMinMaxAverage(self, l):
		print("\tmin: %.02fm" % min(l))
		print("\tmax: %.02fm" % max(l))
		print("\taverage: %.02fm" % statistics.mean(l))
		print("\tmedian: %.02fm" % statistics.median(l))

	def displayResult(self):
		# Count
		print("%d acquisitions" % self.startAcqCount)

		validPercentage = (self.validCount / self.startAcqCount) * 100
		print("\t%d valid points (%.02f%%)" % (self.validCount, validPercentage))

		timeoutPercentage = (self.timeoutCount / self.startAcqCount) * 100
		print("\t%d timeout (%.02f%%)" % (self.timeoutCount, timeoutPercentage))

		# Time to fix
		print("Time to fix")
		print("\tmin: %ds" % min(self.acqDurationList))
		print("\tmax: %ds" % max(self.acqDurationList))
		print("\taverage: %ds" % statistics.mean(self.acqDurationList))
		print("\tmedian: %ds" % statistics.median(self.acqDurationList))

		# Accuracy
		print("Accuracy of RAW points")
		self.displayMinMaxAverage(self.rawAccuracyList)

		print("Accuracy of valid points")
		self.displayMinMaxAverage(self.validAccuracyList)

		print("Accuracy of last points before timeout")
		self.displayMinMaxAverage(self.timeoutAccuracyList)

class GpsHandler:
	def __init__(self, sinks):
		self.sinks = sinks

		self.recordDescList = {
			"StartAcq":   Record(handler=self.handleStartAcq,   hasArgs=False),
			"StopAcq":    Record(handler=self.handleStopAcq,    hasArgs=False),
			"Timeout":    Record(handler=self.handleTimeout,    hasArgs=False),
			"ValidPoint": Record(handler=self.handleValidPoint, hasArgs=False),
			"Location":   Record(handler=self.handleLocation,   hasArgs=True),
		}

	def handleRecord(self, ts, recordStr):
		parseAndHandleRecord(ts, recordStr, self.recordDescList)

	def handleStartAcq(self, ts):
		for sink in self.sinks:
			sink.startAcq(ts)

	def handleStopAcq(self, ts):
		pass

	def handleTimeout(self, ts):
		for sink in self.sinks:
			sink.timeout(ts)

	def handleValidPoint(self, ts):
		for sink in self.sinks:
			sink.validPoint(ts)

	def handleLocation(self, ts, args):
		location = locationFromStr(args)

		for sink in self.sinks:
			sink.newLocation(ts, location)

def msToStrHours(duration):
		duration = int(duration / 1000)
		hours = duration // 3600
		minutes = (duration % 3600) // 60
		seconds = duration % 60

		return "%02d:%02d:%02d" % (hours, minutes, seconds)

def parseTelemetryFile(path):
	# Extra stats
	gpsGeneralStats = GpsGeneralStats()
	gpsAccuracyStats = GpsAccuracyStats()

	gpsSinks = [gpsGeneralStats, gpsAccuracyStats]

	appHandler = AppHandler()
	gpsHandler = GpsHandler(sinks=gpsSinks)
	batteryHandler = BatteryHandler()

	# Parse
	handlerMap = {
		"APP": appHandler,
		"GPS": gpsHandler,
		"Battery": batteryHandler,
	}

	firstTs = None
	lastTs = None

	f = open(path)
	for line in f:
		if line[-1] == '\n':
			line = line[:-1]

		if len(line) == 0:
			break

		# [1492336069275]GPS:
		pattern = re.compile("\[(?P<ts>\d+)\](?P<tag>.*?):")
		result = pattern.match(line)

		# Skip trailing ':'
		endIdx = result.end(result.lastgroup) + 1
		record = line[endIdx:]

		ts = int(result.group("ts"))
		if not firstTs:
			firstTs = ts

		lastTs = ts

		tag = result.group("tag")

		# Call handler
		handler = handlerMap[tag]
		handler.handleRecord(ts, record)

	# Display results
	print("Duration: %s" % msToStrHours(lastTs - firstTs))

	appHandler.displayResult()
	gpsGeneralStats.displayResult()
	batteryHandler.displayResult()

	accuracyChartPath = os.path.dirname(path) + "/accuracyChart.html"
	print("Generate accuracy chart: %s" % accuracyChartPath)
	gpsAccuracyStats.genChart(accuracyChartPath)

parseTelemetryFile(sys.argv[1])


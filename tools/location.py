import re
from collections import namedtuple

Location = namedtuple('Location', ['ts', 'lat', 'lon', 'accuracy', 'speed'])

def convertStrFloat(s):
	return float(s.replace(',', '.'))

def locationFromStr(s):
		pattern = re.compile("ts:(?P<ts>\d*?);lat:(?P<lat>\d*?,\d*?);long:(?P<long>\d*?,\d*?);accuracy:(?P<accuracy>\d*?,\d*?);speed:(?P<speed>\d*?,\d*?)")
		result = pattern.match(s)
		if not result:
			return None

		return Location(
			ts=int(result.group("ts")),
			lat=convertStrFloat(result.group("lat")),
			lon=convertStrFloat(result.group("long")),
			accuracy=convertStrFloat(result.group("accuracy")),
			speed=convertStrFloat(result.group("speed"))
		)


import re
import datetime
import math
from collections import namedtuple

Location = namedtuple('Location', ['ts', 'date', 'lat', 'lon', 'accuracy', 'speed'])

def convertStrFloat(s):
	return float(s.replace(',', '.'))

def formatTimezone(timezone):
	if timezone == 0:
		strTimezone = "GMT"
	else:
		if timezone > 0:
			sign = "+"
		else:
			sign = "-"

		strTimezone = "GMT{0}{1}".format(sign, int(math.fabs(timezone)))

	return strTimezone

def formatDate(ts, timezone):
	# Apply timezone if available
	if timezone:
		ts += timezone * 3600

	date = datetime.datetime.utcfromtimestamp(ts)

	strTs = "{0:04d}/{1:02d}/{2:02d} {3:02d}:{4:02d}:{5:02d}" \
				.format(date.year, date.month, date.day, \
						date.hour, date.minute, date.second)

	# Append timezone if available
	if timezone:
		strTimezone = formatTimezone(timezone)
		strTs += " ({0})".format(strTimezone)

	return strTs

def locationFromStr(s, timezone=None):
		pattern = re.compile("ts:(?P<ts>\d*?);lat:(?P<lat>-?\d*?,\d*?);long:(?P<long>-?\d*?,\d*?);accuracy:(?P<accuracy>\d*?,\d*?);speed:(?P<speed>\d*?,\d*?)")
		result = pattern.match(s)
		if not result:
			return None

		# ms => s
		ts = int(result.group("ts")) // 1000

		return Location(
			ts=ts,
			date=formatDate(ts, timezone),
			lat=convertStrFloat(result.group("lat")),
			lon=convertStrFloat(result.group("long")),
			accuracy=convertStrFloat(result.group("accuracy")),
			speed=convertStrFloat(result.group("speed"))
		)


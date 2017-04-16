#!/bin/bash

docker run -t --rm -v `readlink -f $1`:/output android-build


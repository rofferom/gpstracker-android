#!/bin/bash

USERNAME=user
useradd -m -s /bin/bash -u `stat -c '%u' .` ${USERNAME}

su ${USERNAME} -c "git clone https://github.com/rofferom/gpstracker-android.git ."
su ${USERNAME} -c "./gradlew assemble assembleDebug"


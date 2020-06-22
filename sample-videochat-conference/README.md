# ConnectyCube Android WebRTC Conference code sample for Android, Java

This README introduces [ConnectyCube](https://connectycube.com) Conference code sample written in Java.

Project provides a convinient way for ConnectyCube WebRTC users to take part in video chat between 10-12 people.

Original integration guide and API documentation - [https://developers.connectycube.com/android/videocalling-conference](https://developers.connectycube.com/android/videocalling-conference)

## Features
* Video/Audio Conference with 10-12 people
* Join-Rejoin video room functionality (like Skype)
* Mute/Unmute audio/video stream (own and opponents)
* Display bitrate
* Switch video input device (camera)

## Setup

1. Register new account and application at `https://admin<app_name>.connectycube.com` then put Application credentials from 'Overview' page to the `App` class.

Also, provide your API server and Chat server endpoints at `App` class to point the sample against your own server:

2. Provide your Multiparty Video Conferencing server endpoint at `App` class via `JANUS_SERVER_URL` variable.

## Issues

Please report any issues you find at [GitHub issues page](https://github.com/ConnectyCube/connectycube-android-samples/issues)

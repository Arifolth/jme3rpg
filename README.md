# ANJRpg
Alexander's Nilov Java Role Playing Game (ANJRpg).
An Open Source Action RPG game written in Java.

mailto: <arifolth@gmail.com>

#
![1-BlueMountain.jpg](Screenshots/1-BlueMountain.jpg)
## Features:
- Modular Maven build
- Borderless procedurally generated world (Work In Progress)
- Day and Night Cycle 
- Weather effects (Work In Progress)
- Physics 
- Enemies with AI and Combat System
- Animated models
- Environmental and combat sounds
- UI
- Loading Screen and Main Menu
- Native Executable Module (executable with bundled jre)

## Gameplay Keys
Default bindings:
- W - move forward
- S - move backwards
- A - move left
- D - move right
- SHIFT + W or SHIFT + S - RUN
- LEFT_MOUSE - attack
- MIDDLE_MOUSE - hold to move camera around the player
- RIGHT_MOUSE - block (hold)
- SPACE - jump

## Hardware requirements
- Monitor with at least 1920x1080 resolution
- Decent videocard with 2Gb of Video RAM

For better performance you may want to run the game on a dedicated gaming video card specifically, if you have one.

## Build
Use [Maven](https://maven.apache.org/) to build the ANJRpg. 
JDK 17+ is required to build and run the game.

```bash
mvn install
```

### Native executables
Native executables ware tested to build and run successfully on a Windows box.

To build them enable profile 
```native``` and perform build of the [NativeExecutable](NativeExecutable) module  

## Run using Jdk
```bash
mvn exec:java -pl GameClient -Dexec.jvmArguments="-Xms8G -Xmx8G -server -XX:UseAVX=2 -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -XX:+ParallelRefProcEnabled -XX:G1HeapRegionSize=4m -XX:InitiatingHeapOccupancyPercent=30 -XX:+UseStringDeduplication -XX:+UseCompressedOops -XX:+UseLargePages -XX:LargePageSizeInBytes=2M"
```

## Repo
<https://github.com/Arifolth/jme3rpg>

## Third-party components, libraries and models used
Please see [CREDITS.md](CREDITS.md) 

## Screenshots
![Image](Screenshots/Combat.jpg "icon")
![Image](Screenshots/Rain.jpg "icon")
![Image](Screenshots/Dawn.jpg "icon")
![Image](Screenshots/Lake.jpg "icon")
![Image](Screenshots/Underwater.jpg "icon")
![Image](Screenshots/Night.jpg "icon")
![Image](Screenshots/Grass.jpg "icon")

## License
It is released under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.txt)

#### Copyright 2014 - 2025 &copy; Alexander Nilov
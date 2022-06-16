# ANJRpg
Alexander's Nilov Java Role Playing Game (ANJRpg).

An Open Source Action RPG game written in Java.

mailto: <arifolth@gmail.com>

#
![Image](Screenshot_sunset.jpg "icon")

## Build

Use [Maven](https://maven.apache.org/) to build the ANJRpg.

```bash
mvn install
```
## Run
```bash
mvn exec:java -pl GameClient -Djvm.options="-XX:-TieredCompilation -XX:TieredStopAtLevel=3 -server -XX:+UnlockExperimentalVMOptions -XX:+UseZGC"
```
## About game
Features:
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

## Repo

<https://github.com/Arifolth/jme3rpg>

## Materials used
- [Mtnrim_v0.zip](https://sourceforge.net/projects/mountainrim/)
- https://github.com/rdok/Arcem-Tutari
- Sounds from https://freesound.org/
- Other resources, credit is given in the code

## License
[GPLv3](https://www.gnu.org/licenses/gpl-3.0.txt)

#### Copyright 2022 &copy; Alexander Nilov
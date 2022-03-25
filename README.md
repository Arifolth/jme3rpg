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
## Repo

<https://github.com/Arifolth/jme3rpg>

## License
[GPLv3](https://www.gnu.org/licenses/gpl-3.0.txt)

#### Copyright 2021 &copy; Alexander Nilov
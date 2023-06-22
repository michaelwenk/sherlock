[![DOI](https://zenodo.org/badge/315139777.svg)](https://zenodo.org/badge/latestdoi/315139777) [![License](https://img.shields.io/badge/License-MIT%202.0-blue.svg)](https://opensource.org/licenses/MIt)
[![GitHub contributors](https://img.shields.io/github/contributors/michaelwenk/sherlock.svg)](https://github.com/michaelwenk/sherlock/graphs/contributors/) [![GitHub issues](https://img.shields.io/github/issues/michaelwenk/sherlock.svg)](https://github.com/michaelwenk/sherlock/issues/) [![GitHub release](https://img.shields.io/github/release/michaelwenk/sherlock.svg)](https://github.com/michaelwenk/sherlock/releases/)

<img width="150" alt="sherlock-logo" src="public/Sherlock.png" align="right">

# Sherlock
Web services for Computer-Assisted Structure Elucidation (CASE).

A [graphical user interface](https://github.com/michaelwenk/sherlock-frontend) and [publication](https://doi.org/10.3390/molecules28031448) are available.



## Core  Features
- Dereplication
- Elucidation
- Retrieval of previously generated results 

The dereplication, chemical shift prediction and statistical detection of structural constraints are enabled by using entries from NMRShiftDB and COCONUT containing structural and spectral properties. Spectra in use are both experimental and predicted.  

[casekit](https://github.com/michaelwenk/casekit) serves as computational library within Sherlock.

[PyLSD](https://github.com/nuzillard/PyLSD) is used for structure generation. 
<!---
See [Dependencies](#dependencies) section.
-->

## Docker and Execution of pre-built Containers
This project uses Docker containers (https://www.docker.com) and starts them via docker-compose. Make sure that docker-compose is installed.

NOTE: It is recommended to set the accessible RAM to 6 GB or higher and the number of available CPU cores to two. This can be done easily in the Docker Desktop application, see [here](/public/Docker_settings.png).


### Download
Clone this repository and change the directory:
 
    git clone https://github.com/michaelwenk/sherlock.git
    cd sherlock

Now pull all the containers needed for execution from Docker Hub:

     docker-compose -f docker-compose.yml -f docker-compose.publish.yml pull

### Create and Start
To create the network and start the services for the first time (in detached mode) use:

    docker-compose -f docker-compose.yml -f docker-compose.publish.yml up -d

Note: It will take several minutes until all services are available and registered, i.e. due to the extraction of the compressed fragment data.

If the container network was already created beforehand and stopped via "stop" command, then the "start" command can be used. 
This will avoid extracting the fragments again and the services should be ready within a few seconds.

    docker-compose -f docker-compose.yml -f docker-compose.publish.yml start

### Stop and Removal
To stop the application use:

    docker-compose -f docker-compose.yml -f docker-compose.publish.yml stop

If the removal of the network created by docker-compose is desired, then use the down command:

    docker-compose -f docker-compose.yml -f docker-compose.publish.yml down

